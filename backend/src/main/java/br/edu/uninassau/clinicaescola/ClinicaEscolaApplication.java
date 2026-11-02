package br.edu.uninassau.clinicaescola;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// O pacote principal permite que o Spring encontre as camadas da aplicação.
public class ClinicaEscolaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicaEscolaApplication.class, args);
    }

}
