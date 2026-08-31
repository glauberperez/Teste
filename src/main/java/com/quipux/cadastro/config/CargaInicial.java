package com.quipux.cadastro.config;

import com.quipux.cadastro.pessoa.Pessoa;
import com.quipux.cadastro.pessoa.PessoaRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class CargaInicial {
    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    @Bean
    public CommandLineRunner carregarPessoasDeExemplo(PessoaRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(List.of(
                    new Pessoa("52998224725", "Nathaniel", "Silva", "nathaniel.silva@exemplo.com"),
                    new Pessoa("11144477735", "Giovanni", "Rossi", "giovanni.rossi@exemplo.com"),
                    new Pessoa("39053344705", "Ingrid", "Muller", "ingrid.muller@exemplo.com")));
            log.info("Carga inicial concluida: {} pessoas de exemplo", repository.count());
        };
    }
}
