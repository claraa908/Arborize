package br.com.arborize.integration;

import br.com.arborize.controllers.*;
import br.com.arborize.dtos.response.FotoArvoreResponseDTO;
import br.com.arborize.services.FotoArvoreService;
import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.util.unit.DataSize;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Tomcat/Servlet multipart reais, em loopback, com Service simulado e SEM infraestrutura da aplicação.
 * Fixture funcional sem filtros; proteção administrativa é verificada separadamente com SecurityConfig real. */
class FotoArvoreMultipartIT {
    @TempDir Path folder;
    private Tomcat tomcat;
    private String endpoint;
    private AnnotationConfigWebApplicationContext context;
    @BeforeEach void start() throws Exception {
        var properties=new Properties();
        try(var stream=getClass().getClassLoader().getResourceAsStream("application.properties")){properties.load(stream);}
        var defaults=new org.springframework.boot.servlet.autoconfigure.MultipartProperties();
        long fileSize=DataSize.parse(properties.getProperty("spring.servlet.multipart.max-file-size",defaults.getMaxFileSize().toString())).toBytes();
        long requestSize=DataSize.parse(properties.getProperty("spring.servlet.multipart.max-request-size",defaults.getMaxRequestSize().toString())).toBytes();
        tomcat=new Tomcat();tomcat.setBaseDir(folder.toString());tomcat.setPort(0);tomcat.getConnector().setProperty("address","127.0.0.1");
        tomcat.getConnector().setProperty("maxSwallowSize",Long.toString(DataSize.parse(
                properties.getProperty("server.tomcat.max-swallow-size","2MB")).toBytes()));
        var ctx=tomcat.addContext("",folder.toString());context=new AnnotationConfigWebApplicationContext();context.register(WebConfig.class);
        var wrapper=Tomcat.addServlet(ctx,"dispatcher",new DispatcherServlet(context));wrapper.setLoadOnStartup(1);
        wrapper.setMultipartConfigElement(new MultipartConfigElement(folder.toString(),fileSize,requestSize,0));ctx.addServletMappingDecoded("/","dispatcher");
        tomcat.start();endpoint="http://127.0.0.1:"+tomcat.getConnector().getLocalPort();
    }
    @AfterEach void stop() throws Exception {if(tomcat!=null){tomcat.stop();tomcat.destroy();}if(context!=null)context.close();}
    private HttpResponse<String> upload(int size) throws Exception {
        String boundary="inventario-t20b-boundary";
        byte[] head=("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"foto.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] tail=("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.US_ASCII);
        var body=new byte[head.length+size+tail.length];System.arraycopy(head,0,body,0,head.length);System.arraycopy(tail,0,body,head.length+size,tail.length);
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(endpoint+"/api/arvores/42/fotos"))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Content-Type","multipart/form-data; boundary="+boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void transporteAceitaDoisMiBMaisOverheadMultipart() throws Exception {
        var response=upload(2*1024*1024);assertEquals(201,response.statusCode(),response.body());
        assertTrue(response.body().contains("2097152"));
    }
    @Test void excessoDoLimiteDoServidorRetornaProblema413AntesDoService() throws Exception {
        var response=upload(10*1024*1024+1);assertEquals(413,response.statusCode(),response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"),response.body());
        assertTrue(response.body().contains("fotografia-muito-grande"),response.body());
        verifyNoInteractions(context.getBean(FotoArvoreService.class));
    }
    @TestConfiguration(proxyBeanMethods=false)
    @EnableWebMvc
    @Import({FotoArvoreController.class,GlobalExceptionHandler.class,FotoArvoreMultipartExceptionResolver.class})
    static class WebConfig {
        @Bean StandardServletMultipartResolver multipartResolver(){return new StandardServletMultipartResolver();}
        @Bean FotoArvoreService fotoArvoreService(){
            var service=mock(FotoArvoreService.class);
            when(service.adicionar(anyLong(),any())).thenAnswer(inv->new FotoArvoreResponseDTO(7L,inv.getArgument(0),"image/png",
                    ((org.springframework.web.multipart.MultipartFile)inv.getArgument(1)).getSize(),LocalDateTime.of(2026,1,1,12,0)));
            return service;
        }
    }
}
