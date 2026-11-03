package br.com.arborize.integration;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Proxy TCP/HTTP de teste: encaminha tráfego ao MinIO real preservando Host/assinatura S3. */
final class MinioFaultProxy implements AutoCloseable {
    enum Mode { NORMAL, DENY_PUT, DENY_DELETE, GATE_PUT, GATE_DELETE }
    volatile Mode mode = Mode.NORMAL;
    volatile CountDownLatch reached = new CountDownLatch(1), release = new CountDownLatch(1);
    final AtomicInteger denied = new AtomicInteger();
    private final ServerSocket server;
    private final URI target;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private volatile boolean closed;

    MinioFaultProxy(URI target) throws IOException {
        this.target=target;server=new ServerSocket(0,50,InetAddress.getByName("127.0.0.1"));
        workers.submit(()-> {while(!closed) try {Socket client=server.accept();workers.submit(()->serve(client));}
                            catch(IOException e) {if(!closed) throw new UncheckedIOException(e);}});
    }
    String endpoint() {return "http://127.0.0.1:"+server.getLocalPort();}
    void reset() {release.countDown();mode=Mode.NORMAL;reached=new CountDownLatch(1);release=new CountDownLatch(1);denied.set(0);}
    private void serve(Socket client) {
        try(client) {
            client.setSoTimeout(20000);
            var header=new ByteArrayOutputStream();var in=client.getInputStream();int value;
            while(header.size()<65536 && (value=in.read())!=-1) {
                header.write(value);byte[] h=header.toByteArray();int n=h.length;
                if(n>=4 && h[n-4]=='\r' && h[n-3]=='\n' && h[n-2]=='\r' && h[n-1]=='\n') break;
            }
            String raw=header.toString(StandardCharsets.ISO_8859_1);
            String[] line=raw.split("\r\n",2)[0].split(" ");if(line.length<2) return;
            boolean object=line[1].split("\\?",2)[0].split("/").length>=3;
            Mode current=mode;
            if(object && ((current==Mode.DENY_PUT && line[0].equals("PUT")) || (current==Mode.DENY_DELETE && line[0].equals("DELETE")))) {
                denied.incrementAndGet();byte[] body="<Error><Code>AccessDenied</Code><Message>Controlled transport rejection</Message><RequestId>test</RequestId></Error>".getBytes(StandardCharsets.UTF_8);
                client.getOutputStream().write(("HTTP/1.1 403 Forbidden\r\nConnection: close\r\nContent-Type: application/xml\r\nContent-Length: "+body.length+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                client.getOutputStream().write(body);return;
            }
            try(Socket upstream=new Socket(target.getHost(),target.getPort())) {
                upstream.setSoTimeout(20000);
                raw=raw.replaceAll("(?im)^Connection:[^\\r\\n]*\\r\\n", "");
                raw=raw.substring(0,raw.length()-2)+"Connection: close\r\n\r\n";
                upstream.getOutputStream().write(raw.getBytes(StandardCharsets.ISO_8859_1));
                workers.submit(()-> {try {in.transferTo(upstream.getOutputStream());} catch(IOException ignored) {}});
                boolean gate=object && ((current==Mode.GATE_PUT && line[0].equals("PUT")) || (current==Mode.GATE_DELETE && line[0].equals("DELETE")));
                if(gate) {
                    byte[] response=upstream.getInputStream().readAllBytes();
                    if(new String(response,0,Math.min(response.length,40),StandardCharsets.US_ASCII).matches("(?s)^HTTP/1\\.[01] 2.*")) {
                        reached.countDown();if(!release.await(20,TimeUnit.SECONDS)) throw new IOException("Gate timeout");
                    }
                    client.getOutputStream().write(response);
                } else upstream.getInputStream().transferTo(client.getOutputStream());
            }
        } catch(IOException ignored) { /* Fechamento deliberado, inclusive no teste de interrupção. */ }
          catch(InterruptedException e) {Thread.currentThread().interrupt();}
    }
    @Override public void close() throws IOException {closed=true;release.countDown();server.close();workers.shutdownNow();}
}
