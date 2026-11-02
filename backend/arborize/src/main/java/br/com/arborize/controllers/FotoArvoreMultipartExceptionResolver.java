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
import java.util.List;
import java.util.Map;

/** O parser multipart pode falhar antes da seleção do Controller e de seu advice restrito. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FotoArvoreMultipartExceptionResolver implements HandlerExceptionResolver {
    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                        Object handler, Exception exception) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!(exception instanceof MaxUploadSizeExceededException)
                || !"POST".equals(request.getMethod())
                || !path.matches("/api/arvores/-?[0-9]+/fotos/?")) return null;

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,
                "O envio excede o limite de tamanho permitido. Cada fotografia deve ter até 2 MB.");
        problem.setTitle("Requisição não concluída");
        problem.setType(URI.create("urn:arborize:inventario:fotografia-muito-grande"));
        problem.setProperty("code", "fotografia-muito-grande");
        problem.setProperty("errors", List.of(Map.of("field", "file", "message", problem.getDetail())));
        response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        try {
            converter.write(problem, MediaType.APPLICATION_PROBLEM_JSON, new ServletServerHttpResponse(response));
        } catch (IOException disconnectedClient) {
            // A conexão já não aceita a resposta; não expor detalhes nem executar o Controller.
        }
        return new ModelAndView();
    }
}
