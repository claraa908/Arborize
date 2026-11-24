package br.com.arborize.controllers;

import br.com.arborize.dtos.response.SobreIndicadoresResponseDTO;
import br.com.arborize.services.SobreIndicadoresService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SobreControllerTest {

    @Mock
    private SobreIndicadoresService sobreIndicadoresService;

    @InjectMocks
    private SobreController controller;

    @Test
    void deveRetornar200ComIndicadoresPublicos() {
        SobreIndicadoresResponseDTO esperado = new SobreIndicadoresResponseDTO(120L, 450L, 980L, 18L);
        when(sobreIndicadoresService.obterIndicadores()).thenReturn(esperado);

        ResponseEntity<SobreIndicadoresResponseDTO> resposta = controller.obterIndicadores();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEqualTo(esperado);
        verify(sobreIndicadoresService).obterIndicadores();
    }
}
