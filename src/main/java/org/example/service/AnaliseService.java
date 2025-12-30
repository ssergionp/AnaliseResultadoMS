package org.example.service;

import org.example.model.Sorteio;
import java.util.*;
import java.util.stream.Collectors;

public class AnaliseService {

    public Map<Integer, Long> calcularFrequencias(List<Sorteio> sorteios) {
        Map<Integer, Long> freq = sorteios.stream()
                .flatMap(s -> s.numeros().stream())
                .collect(Collectors.groupingBy(n -> n, Collectors.counting()));
        for (int i = 1; i <= 60; i++) freq.putIfAbsent(i, 0L);
        return freq;
    }

    public Map<String, Integer> obterTopCombinacoes(List<Sorteio> sorteios, int tamanhoK, int limite) {
        Map<String, Integer> combinacoes = new HashMap<>();
        for (Sorteio s : sorteios) {
            List<Integer> n = s.numeros().stream().sorted().toList();
            gerarCombinacoes(n, tamanhoK, 0, new ArrayList<>(), combinacoes);
        }
        return combinacoes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limite)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private void gerarCombinacoes(List<Integer> n, int k, int start, List<Integer> atual, Map<String, Integer> res) {
        if (atual.size() == k) {
            String combo = atual.toString();
            res.put(combo, res.getOrDefault(combo, 0) + 1);
            return;
        }
        for (int i = start; i < n.size(); i++) {
            atual.add(n.get(i));
            gerarCombinacoes(n, k, i + 1, atual, res);
            atual.remove(atual.size() - 1);
        }
    }

    public Map<String, Long> calcularFrequenciaSequencias(List<Sorteio> sorteios) {
        Map<String, Long> seqMap = new HashMap<>();
        seqMap.put("Sem Sequência", 0L); seqMap.put("Duplas (2)", 0L);
        seqMap.put("Trincas (3)", 0L); seqMap.put("Maiores (4+)", 0L);

        for (Sorteio s : sorteios) {
            List<Integer> nums = s.numeros().stream().sorted().toList();
            int maxSeq = 1, atualSeq = 1;
            for (int i = 0; i < nums.size() - 1; i++) {
                if (nums.get(i + 1) == nums.get(i) + 1) atualSeq++;
                else { maxSeq = Math.max(maxSeq, atualSeq); atualSeq = 1; }
            }
            maxSeq = Math.max(maxSeq, atualSeq);
            if (maxSeq == 1) seqMap.put("Sem Sequência", seqMap.get("Sem Sequência") + 1);
            else if (maxSeq == 2) seqMap.put("Duplas (2)", seqMap.get("Duplas (2)") + 1);
            else if (maxSeq == 3) seqMap.put("Trincas (3)", seqMap.get("Trincas (3)") + 1);
            else seqMap.put("Maiores (4+)", seqMap.get("Maiores (4+)") + 1);
        }
        return seqMap;
    }

    public Map<String, Long> calcularDistribuicaoFamilias(List<Sorteio> sorteios) {
        Map<String, Long> fam = new LinkedHashMap<>();
        String[] labels = {"01-10", "11-20", "21-30", "31-40", "41-50", "51-60"};
        for (String l : labels) fam.put(l, 0L);
        for (Sorteio s : sorteios) {
            for (Integer n : s.numeros()) {
                int idx = Math.min((n - 1) / 10, 5);
                fam.put(labels[idx], fam.get(labels[idx]) + 1);
            }
        }
        return fam;
    }

    public List<Integer> gerarSugestao(List<Sorteio> sorteios, String estrategia, int qtd) {
        if (sorteios.isEmpty()) return Collections.emptyList();
        List<Integer> res = switch (estrategia) {
            case "Quentes (Mais Frequentes)" -> obterPorFreq(sorteios, qtd, true);
            case "Frios (Menos Frequentes)" -> obterPorFreq(sorteios, qtd, false);
            default -> obterPorFreq(sorteios, qtd, true);
        };
        return isPadraoImprovavel(res) ? gerarSugestao(sorteios, estrategia, qtd) : res.stream().sorted().toList();
    }

    private List<Integer> obterPorFreq(List<Sorteio> s, int q, boolean h) {
        Map<Integer, Long> f = calcularFrequencias(s);
        return f.entrySet().stream().sorted(h ? Map.Entry.<Integer, Long>comparingByValue().reversed() : Map.Entry.comparingByValue())
                .limit(q).map(Map.Entry::getKey).toList();
    }

    private boolean isPadraoImprovavel(List<Integer> j) {
        if (j.size() < 6) return false;
        List<Integer> s = j.stream().sorted().toList();
        int cons = 0;
        for (int i = 0; i < s.size()-1; i++) if (s.get(i+1) == s.get(i)+1) cons++;
        return cons > 3 || s.stream().map(n -> (n-1)/10).distinct().count() == 1;
    }

    public double calcularCusto(int d) {
        return switch (d) {
            case 6 -> 6.0; case 7 -> 42.0; case 8 -> 168.0; case 9 -> 504.0;
            case 10 -> 1260.0; case 11 -> 2772.0; case 12 -> 5544.0; case 13 -> 10296.0;
            case 14 -> 18018.0; case 15 -> 30030.0; case 16 -> 48048.0; case 17 -> 74256.0;
            default -> 0.0;
        };
    }

    public Map<String, Double> calcularMediaParesImpares(List<Sorteio> s) {
        if (s.isEmpty()) return Map.of("Pares", 0.0, "Ímpares", 0.0);
        long p = s.stream().mapToLong(Sorteio::contarPares).sum();
        double pp = (double) p / (s.size() * 6) * 100;
        return Map.of("Pares", pp, "Ímpares", 100 - pp);
    }

    public List<String> verificarJogo(List<Sorteio> h, List<Integer> j) {
        List<String> r = new ArrayList<>(); Set<Integer> a = new HashSet<>(j);
        for (Sorteio s : h) {
            long ac = s.numeros().stream().filter(a::contains).count();
            if (ac >= 4) r.add(String.format("Conc. %d (%s): %d acertos [%s]", s.concurso(), s.data(), ac, (ac==6?"SENA":ac==5?"QUINA":"QUADRA")));
        }
        return r;
    }
}
