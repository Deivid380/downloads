package demo.downloads;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class App extends Application {

    private DownloadManager manager;
    private final ObservableList<DownloadTask> observableTasks = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        manager = new DownloadManager(2); // límite inicial: 2 descargas simultáneas

        TableView<DownloadTask> table = createTableView();

        // -------- FORMULARIO PARA AGREGAR DESCARGAS --------
        TextField nameField = new TextField();
        nameField.setPromptText("Nombre archivo");

        TextField sizeField = new TextField();
        sizeField.setPromptText("Tamaño (MB)");

        TextField speedField = new TextField();
        speedField.setPromptText("Velocidad (KB/s)");

        Button addBtn = new Button("Agregar");
        addBtn.setOnAction(e -> {
            String name = nameField.getText().isBlank()
                    ? "archivo_" + (observableTasks.size() + 1)
                    : nameField.getText().trim();

            long sizeMB = parseLongOrDefault(sizeField.getText(), 10);
            int speed = (int) parseLongOrDefault(speedField.getText(), 400);

            DownloadItem item = new DownloadItem(name, sizeMB * 1024L * 1024L, speed);
            DownloadTask t = manager.createAndSubmit(item);

            bindTaskToList(t);

            nameField.clear();
            sizeField.clear();
            speedField.clear();
        });

        HBox addBox = new HBox(8, nameField, sizeField, speedField, addBtn);
        addBox.setPadding(new Insets(8));
        addBox.setAlignment(Pos.CENTER_LEFT);

        // -------- CONTROL DE CONCURRENCIA --------
        Spinner<Integer> concurrencySpinner = new Spinner<>(1, 10, 2);
        concurrencySpinner.valueProperty().addListener((obs, oldV, newV) -> manager.setMaxConcurrent(newV));

        Label concurrencyLbl = new Label("Max concurrentes:");
        HBox concurrencyBox = new HBox(8, concurrencyLbl, concurrencySpinner);
        concurrencyBox.setAlignment(Pos.CENTER_LEFT);

        // -------- LABEL DE ACTIVAS --------
        Label activeLabel = new Label("Activas: 0");

        observableTasks.addListener((ListChangeListener<DownloadTask>) c -> updateActiveLabel(activeLabel));

        // -------- PROGRESO GLOBAL --------
        ProgressBar globalBar = new ProgressBar(0);
        globalBar.setPrefWidth(350);

        DoubleBinding globalProgressBinding = Bindings.createDoubleBinding(() -> {
            List<DownloadTask> list = List.copyOf(observableTasks);

            long total = list.stream().mapToLong(DownloadTask::getTotalBytes).sum();
            if (total == 0) return 0.0;

            long downloaded = list.stream().mapToLong(DownloadTask::getDownloadedBytes).sum();
            return (double) downloaded / (double) total;
        }, observableTasks);

        globalBar.progressProperty().bind(globalProgressBinding);

        HBox statusBar = new HBox(12, concurrencyBox, activeLabel, new Label("Progreso total:"), globalBar);
        statusBar.setPadding(new Insets(8));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(6, addBox, statusBar, table);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 900, 500);
        stage.setScene(scene);
        stage.setTitle("Simulador de Descargas — Avance");
        stage.show();
    }

    // ------------------------------------------------------------
    // TABLEVIEW
    // ------------------------------------------------------------
    private TableView<DownloadTask> createTableView() {
        TableView<DownloadTask> table = new TableView<>(observableTasks);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<DownloadTask, String> nameCol = new TableColumn<>("Archivo");
        nameCol.setCellValueFactory(c -> c.getValue().titleProperty());

        TableColumn<DownloadTask, Double> progCol = new TableColumn<>("Progreso");
        progCol.setCellValueFactory(c -> c.getValue().progressProperty().asObject());
        progCol.setCellFactory(ProgressBarTableCell.forTableColumn());

        TableColumn<DownloadTask, String> sizeCol = new TableColumn<>("Tamaño (MB)");
        sizeCol.setCellValueFactory(c -> Bindings.createStringBinding(() ->
                String.format("%.1f", c.getValue().getTotalBytes() / (1024.0 * 1024.0)),
                c.getValue().downloadedBytesProperty()));

        TableColumn<DownloadTask, String> msgCol = new TableColumn<>("Estado");
        msgCol.setCellValueFactory(c -> c.getValue().messageProperty());

        TableColumn<DownloadTask, Void> actionsCol = new TableColumn<>("Acciones");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button pauseBtn = new Button("Pausar");
            private final Button resumeBtn = new Button("Reanudar");
            private final Button cancelBtn = new Button("Cancelar");
            private final HBox box = new HBox(6, pauseBtn, resumeBtn, cancelBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);

                pauseBtn.setOnAction(e -> {
                    DownloadTask t = getTableView().getItems().get(getIndex());
                    t.pause();
                });

                resumeBtn.setOnAction(e -> {
                    DownloadTask t = getTableView().getItems().get(getIndex());
                    t.resumeTask();
                });

                cancelBtn.setOnAction(e -> {
                    DownloadTask t = getTableView().getItems().get(getIndex());
                    t.cancel();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(nameCol, progCol, sizeCol, msgCol, actionsCol);
        return table;
    }

    // ------------------------------------------------------------
    // BINDINGS Y UTILIDADES
    // ------------------------------------------------------------
    private void bindTaskToList(DownloadTask task) {
        observableTasks.add(task);
        task.stateProperty().addListener((obs, oldState, newState) -> updateActiveLabel(null));
    }

    private void updateActiveLabel(Label activeLabel) {
        if (activeLabel == null) return;

        Platform.runLater(() -> {
            long active = observableTasks.stream()
                    .filter(t -> t.getState() == javafx.concurrent.Worker.State.RUNNING ||
                                 t.getState() == javafx.concurrent.Worker.State.SCHEDULED ||
                                 t.getState() == javafx.concurrent.Worker.State.READY)
                    .count();
            activeLabel.setText("Activas: " + active);
        });
    }

    private long parseLongOrDefault(String text, long fallback) {
        try {
            return Long.parseLong(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void stop() {
        if (manager != null) manager.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
