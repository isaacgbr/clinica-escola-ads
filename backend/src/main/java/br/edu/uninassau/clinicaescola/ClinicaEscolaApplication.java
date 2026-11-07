package br.edu.uninassau.clinicaescola;

import br.edu.uninassau.clinicaescola.cli.ProvisionarPrimeiroAdmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// O pacote principal permite que o Spring encontre as camadas da aplicação.
public class ClinicaEscolaApplication {

    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--provisionar-primeiro-admin")) {
            ProvisionarPrimeiroAdmin.executar(args);
            return;
        }
        SpringApplication.run(ClinicaEscolaApplication.class, args);
    }

}
