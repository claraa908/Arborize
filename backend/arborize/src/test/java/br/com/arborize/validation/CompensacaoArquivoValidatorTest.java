package br.com.arborize.validation;

import br.com.arborize.exceptions.CompensacaoException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CompensacaoArquivoValidatorTest {
    private final CompensacaoArquivoValidator validator = new CompensacaoArquivoValidator(mock(FotoArvoreValidator.class));

    @Test void aceitaPdfValidoDentroDoLimite() throws Exception {
        byte[] pdf;
        try (var document = new PDDocument(); var output = new java.io.ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }
        var arquivo = new MockMultipartFile("arquivo", "termo.PDF", "application/pdf", pdf);
        assertEquals("application/pdf", validator.validarTermo(arquivo).contentType());
    }

    @Test void rejeitaPdfApenasComExtensaoEArquivoAcimaDoLimite() {
        var falsoPdf = new MockMultipartFile("arquivo", "termo.pdf", "application/pdf", "%PDF-nao valido".getBytes());
        assertThrows(CompensacaoException.class, () -> validator.validarTermo(falsoPdf));

        var grande = new MockMultipartFile("arquivo", "termo.pdf", "application/pdf",
                new byte[CompensacaoArquivoValidator.TERMO_MAX_BYTES + 1]);
        assertThrows(CompensacaoException.class, () -> validator.validarTermo(grande));
    }
}
