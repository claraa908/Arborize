package br.com.arborize.integration;

import br.com.arborize.services.FotoArvoreService;
import br.com.arborize.validation.FotoArvoreValidatorTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockMultipartFile;

/** Processo filho exclusivo do teste, interrompido somente após confirmação de escrita/remoção no MinIO. */
public final class FotoArvoreCrashProbe {
    public static void main(String[] args) throws Exception {
        try(var context=new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(InventarioPersistenceTestConfig.PROFILE);
            context.register(InventarioPersistenceTestConfig.class,InventarioMinioTestConfig.class);context.refresh();
            var service=context.getBean(FotoArvoreService.class);long id=Long.parseLong(args[1]);
            if(args[0].equals("upload")) service.adicionar(id,new MockMultipartFile("file","foto.png","image/png",FotoArvoreValidatorTest.imagem("png")));
            else service.remover(id,Long.parseLong(args[2]));
        }
    }
}
