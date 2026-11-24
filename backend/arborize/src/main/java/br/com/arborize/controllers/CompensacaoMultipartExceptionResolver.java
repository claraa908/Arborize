package br.com.arborize.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import java.io.IOException;
import java.net.URI;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CompensacaoMultipartExceptionResolver implements HandlerExceptionResolver {
    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                          Object handler, Exception exception) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean isCompensacaoUpload = path.equals("/api/termos-compensacao")
                || path.matches("/api/termos-compensacao/[0-9]+/entregas");
        if (!(exception instanceof MaxUploadSizeExceededException) || !"POST".equals(request.getMethod())
                || !isCompensacaoUpload) return null;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo excede o limite permitido: 10 MB para o termo e 2 MB para cada recibo.");
        problem.setTitle("Arquivo muito grande");
        problem.setType(URI.create("urn:arborize:compensacao:arquivo-muito-grande"));
        problem.setProperty("code", "arquivo-muito-grande");
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        try {
            converter.write(problem, MediaType.APPLICATION_PROBLEM_JSON, new ServletServerHttpResponse(response));
        } catch (IOException disconnectedClient) {
            // A conexão já foi encerrada pelo cliente; não há resposta adicional segura a enviar.
        }
        return new ModelAndView();
    }
}
