package org.example.service;

import org.example.model.Sorteio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnaliseServiceTest {

    private AnaliseService service;
    private List<Sorteio> dadosTeste;

    @BeforeEach
    void setUp() {
        service = new AnaliseService();
        // Criando dados fictícios para os testes
        // Concurso 1: 3 pares (2,4,6) e 3 ímpares (1,3,5)
        Sorteio s1 = new Sorteio(1, LocalDate.now(), List.of(1, 2, 3, 4, 5, 6));
        // Concurso 2: 6 pares
        Sorteio s2 = new Sorteio(2, LocalDate.now(), List.of(10, 20, 30, 40, 50, 60));

        dadosTeste = List.of(s1, s2);
    }

    @Test
    @DisplayName("Deve calcular corretamente os custos da tabela oficial")
    void deveCalcularCustosCorretamente() {
        assertAll(
                () -> assertEquals(6.0, service.calcularCusto(6), "Custo de 6 dezenas"),
                () -> assertEquals(42.0, service.calcularCusto(7), "Custo de 7 dezenas"),
                () -> assertEquals(30030.0, service.calcularCusto(15), "Custo de 15 dezenas"),
                () -> assertEquals(74256.0, service.calcularCusto(17), "Custo de 17 dezenas"),
                () -> assertEquals(0.0, service.calcularCusto(5), "Quantidade inválida abaixo"),
                () -> assertEquals(0.0, service.calcularCusto(18), "Quantidade inválida acima")
        );
    }

    @Test
    @DisplayName("Deve calcular a média de pares e ímpares corretamente")
    void deveCalcularMediaParesImpares() {
        // Total de números: 12. Pares: 3 (do s1) + 6 (do s2) = 9.
        // 9 / 12 = 0.75 (75%)
        Map<String, Double> medias = service.calcularMediaParesImpares(dadosTeste);

        assertEquals(75.0, medias.get("Pares"), 0.1);
        assertEquals(25.0, medias.get("Ímpares"), 0.1);
    }

    @Test
    @DisplayName("Deve identificar frequências corretamente")
    void deveCalcularFrequenciasCorretamente() {
        Map<Integer, Long> freq = service.calcularFrequencias(dadosTeste);

        assertEquals(1L, freq.get(1), "Número 1 apareceu 1 vez");
        assertEquals(1L, freq.get(60), "Número 60 apareceu 1 vez");
        assertNull(freq.get(99), "Número não sorteado deve ser nulo ou ausente");
    }

    @Test
    @DisplayName("Deve verificar prêmios ganhos corretamente")
    void deveVerificarPremiosGanhos() {
        // Jogo do usuário que acerta a SENA no Concurso 1
        List<Integer> jogoGanhador = List.of(1, 2, 3, 4, 5, 6);
        List<String> premios = service.verificarJogo(dadosTeste, jogoGanhador);

        assertFalse(premios.isEmpty());
        assertTrue(premios.get(0).contains("SENA"), "Deve identificar a SENA no concurso 1");
    }

    @Test
    @DisplayName("Deve identificar QUADRA e QUINA corretamente")
    void deveIdentificarQuadraEQuina() {
        // Jogo com 4 acertos no Concurso 2 (10, 20, 30, 40)
        List<Integer> jogoQuadra = List.of(10, 20, 30, 40, 11, 12);
        List<String> resultados = service.verificarJogo(dadosTeste, jogoQuadra);

        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).contains("QUADRA"));
    }
}
