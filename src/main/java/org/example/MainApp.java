package org.example;

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.model.Sorteio;
import org.example.service.AnaliseService;
import org.example.service.ExcelService;
import java.time.LocalDate;
import java.util.*;

public class MainApp extends Application {
    private final ExcelService excelService = new ExcelService();
    private final AnaliseService analiseService = new AnaliseService();
    private final String PATH = "H:\\Alura\\Java\\Back End Java\\screenmatch\\planilhas\\todos_resultados_mega_sena.xlsx";

    private ObservableList<Sorteio> masterData = FXCollections.observableArrayList();
    private TableView<Sorteio> tabela = new TableView<>();
    private BarChart<String, Number> graphMais, graphMenos, graphFamilias;
    private PieChart graphPizza, graphSequencias;
    private DatePicker dpI, dpF;
    private ComboBox<String> cbParidade;
    private TextArea txtTopPares, txtTopTrincas;

    @Override
    public void start(Stage stage) {
        stage.setTitle("BI Mega-Sena Analytics v4.0 Pro");
        try {
            List<Sorteio> dados = excelService.lerResultados(PATH);
            masterData.addAll(dados);

            TabPane tabPane = new TabPane();
            tabPane.getTabs().add(new Tab("Dashboard & BI", criarAbaDashboard()));
            tabPane.getTabs().add(new Tab("Gerador de Sugestões", criarAbaSugestoes()));
            tabPane.getTabs().add(new Tab("Verificador de Prêmios", criarAbaVerificador()));
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            stage.setScene(new Scene(tabPane, 1366, 768));
            stage.show();
            atualizarUI(dados);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox criarAbaDashboard() {
        dpI = new DatePicker(LocalDate.of(1996, 3, 11));
        dpF = new DatePicker(LocalDate.now());
        cbParidade = new ComboBox<>(FXCollections.observableArrayList("Todos", "Concursos Pares", "Concursos Ímpares"));
        cbParidade.setValue("Todos");

        Button btnF = new Button("Aplicar Filtros");
        btnF.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white;");

        HBox hbFiltros = new HBox(15, new Label("De:"), dpI, new Label("Até:"), dpF, new Label("Concurso:"), cbParidade, btnF);
        hbFiltros.setPadding(new Insets(10)); hbFiltros.setAlignment(Pos.CENTER_LEFT);
        hbFiltros.setStyle("-fx-background-color: #f1f3f4;");

        configurarTabela();

        // Área de Gráficos com FlowPane para evitar quebra de labels
        FlowPane flowGraficos = new FlowPane(20, 20);
        flowGraficos.setPadding(new Insets(15));
        flowGraficos.setPrefWrapLength(1000); // Tenta manter dois por linha se possível

        graphMais = criarGrafico("Top 10 Quentes", 400);
        graphMenos = criarGrafico("Top 10 Frios", 400);
        graphFamilias = criarGrafico("Famílias (Dezenas)", 400);
        graphPizza = criarPizza("Pares vs Ímpares");
        graphSequencias = criarPizza("Sequências");

        txtTopPares = new TextArea(); txtTopPares.setEditable(false); txtTopPares.setPrefSize(300, 150);
        txtTopTrincas = new TextArea(); txtTopTrincas.setEditable(false); txtTopTrincas.setPrefSize(300, 150);

        VBox vCombos = new VBox(10, new Label("Top 5 Pares Frequentes:"), txtTopPares, new Label("Top 5 Trincas Frequentes:"), txtTopTrincas);

        flowGraficos.getChildren().addAll(graphMais, graphMenos, graphFamilias, graphPizza, graphSequencias, vCombos);

        ScrollPane scroll = new ScrollPane(flowGraficos);
        scroll.setFitToWidth(true);

        HBox mainLayout = new HBox(10, tabela, scroll);
        HBox.setHgrow(scroll, Priority.ALWAYS);

        btnF.setOnAction(e -> filtrar());
        return new VBox(hbFiltros, mainLayout);
    }

    private BarChart<String, Number> criarGrafico(String t, double w) {
        BarChart<String, Number> bc = new BarChart<>(new CategoryAxis(), new NumberAxis());
        bc.setTitle(t); bc.setAnimated(false); bc.setLegendVisible(false); bc.setPrefSize(w, 280);
        return bc;
    }

    private PieChart criarPizza(String t) {
        PieChart pc = new PieChart(); pc.setTitle(t); pc.setAnimated(false); pc.setPrefSize(400, 280);
        return pc;
    }

    private void atualizarUI(List<Sorteio> d) {
        atualizarBar(graphMais, d, true); atualizarBar(graphMenos, d, false);

        graphFamilias.getData().clear();
        XYChart.Series<String, Number> sf = new XYChart.Series<>();
        analiseService.calcularDistribuicaoFamilias(d).forEach((k, v) -> sf.getData().add(new XYChart.Data<>(k, v)));
        graphFamilias.getData().add(sf);

        Map<String, Double> m = analiseService.calcularMediaParesImpares(d);
        graphPizza.setData(FXCollections.observableArrayList(new PieChart.Data("Pares", m.get("Pares")), new PieChart.Data("Ímpares", m.get("Ímpares"))));

        Map<String, Long> s = analiseService.calcularFrequenciaSequencias(d);
        graphSequencias.setData(FXCollections.observableArrayList(s.entrySet().stream().map(e -> new PieChart.Data(e.getKey(), e.getValue())).toList()));

        txtTopPares.setText(formatarCombo(analiseService.obterTopCombinacoes(d, 2, 5)));
        txtTopTrincas.setText(formatarCombo(analiseService.obterTopCombinacoes(d, 3, 5)));
    }

    private String formatarCombo(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append(" -> ").append(v).append(" vezes\n"));
        return sb.toString();
    }

    // ... (restante dos métodos de filtragem, tabela, sugestão e verificador iguais à v3.0) ...
// ... existing code ...
    private void filtrar() {
        List<Sorteio> f = masterData.stream()
                .filter(s -> !s.data().isBefore(dpI.getValue()) && !s.data().isAfter(dpF.getValue()))
                .filter(s -> {
                    if (cbParidade.getValue().equals("Concursos Pares")) return s.concurso() % 2 == 0;
                    if (cbParidade.getValue().equals("Concursos Ímpares")) return s.concurso() % 2 != 0;
                    return true;
                }).toList();
        tabela.setItems(FXCollections.observableArrayList(f));
        atualizarUI(f);
    }

    private void atualizarBar(BarChart<String, Number> bc, List<Sorteio> d, boolean h) {
        bc.getData().clear(); XYChart.Series<String, Number> s = new XYChart.Series<>();
        Map<Integer, Long> f = analiseService.calcularFrequencias(d);
        f.entrySet().stream().sorted(h ? Map.Entry.<Integer, Long>comparingByValue().reversed() : Map.Entry.comparingByValue())
                .limit(10).forEach(e -> s.getData().add(new XYChart.Data<>(e.getKey().toString(), e.getValue())));
        bc.getData().add(s);
    }

    private void configurarTabela() {
        TableColumn<Sorteio, Integer> c1 = new TableColumn<>("Conc.");
        c1.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().concurso()));
        TableColumn<Sorteio, LocalDate> c2 = new TableColumn<>("Data");
        c2.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().data()));
        TableColumn<Sorteio, List<Integer>> c3 = new TableColumn<>("Dezenas");
        c3.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().numeros()));
        tabela.getColumns().addAll(c1, c2, c3);
    }

    private VBox criarAbaSugestoes() {
        ComboBox<String> cbMetodo = new ComboBox<>(FXCollections.observableArrayList("Quentes (Mais Frequentes)", "Frios (Menos Frequentes)"));
        cbMetodo.setValue("Quentes (Mais Frequentes)");
        ComboBox<Integer> cbQtd = new ComboBox<>(FXCollections.observableArrayList(6,7,8,9,10,11,12,13,14,15));
        cbQtd.setValue(6);
        Label lblRes = new Label("Sugestão:");
        Button btn = new Button("Gerar Sugestão");
        btn.setOnAction(e -> lblRes.setText("Sugestão: " + analiseService.gerarSugestao(tabela.getItems(), cbMetodo.getValue(), cbQtd.getValue())));
        return new VBox(20, cbMetodo, cbQtd, btn, lblRes);
    }

    private VBox criarAbaVerificador() {
        TextField txt = new TextField(); Button btn = new Button("Verificar"); TextArea res = new TextArea();
        btn.setOnAction(e -> {
            try {
                List<Integer> j = Arrays.stream(txt.getText().split(",")).map(String::trim).map(Integer::parseInt).toList();
                res.setText(String.join("\n", analiseService.verificarJogo(masterData, j)));
            } catch (Exception ex) { res.setText("Erro."); }
        });
        return new VBox(10, txt, btn, res);
    }

    public static void main(String[] args) { launch(args); }
}
