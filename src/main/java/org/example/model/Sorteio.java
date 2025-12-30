package org.example.model;

import java.time.LocalDate;
import java.util.List;

public record Sorteio(int concurso, LocalDate data, List<Integer> numeros) {
    public boolean isPar(int numero) { return numero % 2 == 0; }

    public long contarPares() {
        return numeros.stream().filter(this::isPar).count();
    }
}
