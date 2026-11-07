package br.edu.uninassau.clinicaescola.repository;

import br.edu.uninassau.clinicaescola.entity.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class PerfilMapeamentoTest {
    @Test void sqlIncrementalEJpaUsamOsMesmosPerfisETipoTexto() throws Exception {
        var campo = Usuario.class.getDeclaredField("perfil");
        assertEquals(EnumType.STRING, campo.getAnnotation(Enumerated.class).value());
        assertEquals(7, campo.getAnnotation(Column.class).length());
        assertFalse(campo.getAnnotation(Column.class).nullable());
        assertArrayEquals(new Perfil[]{Perfil.ADMIN, Perfil.INTERNO}, Perfil.values());
        String sql = Files.readString(Path.of("sql/002_adicionar_perfil_usuarios.sql"));
        assertTrue(sql.contains("ALTER TABLE public.usuarios"));
        assertTrue(sql.contains("perfil VARCHAR(7) NOT NULL DEFAULT 'INTERNO'"));
        assertTrue(sql.contains("CHECK (perfil IN ('ADMIN', 'INTERNO'))"));
        assertFalse(sql.matches("(?is).*\\b(DROP|DELETE|INSERT|UPDATE|TRUNCATE)\\b.*"));
    }
}
