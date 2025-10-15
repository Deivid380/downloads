package demo.downloads;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application {

    private DownloadManager manager;

    @Override
    public void start(Stage stage) {
        // Máximo de descargas simultáneas (ajustable)
        manager = new DownloadManager(2);

        // Tabla
        TableView<DownloadTask> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<DownloadTask, String> nameCol = new TableColumn<>("Archivo");
        nameCol.setCellValueFactory(c -> c.getValue().titleProperty());

        TableColumn<DownloadTask, Double> progCol = new TableColumn<>("Progreso");
        progCol.setCellValueFactory(c -> c.getValue().progressProperty().asObject());
        progCol.setCellFactory(ProgressBarTableCell.forTableColumn());

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

        table.getColumns().addAll(nameCol, progCol, msgCol, actionsCol);

        // Controles superiores: agregar descargas y configurar concurrencia (en este avance el límite es fijo)
        Button add3Btn = new Button("Agregar 3 descargas demo");
        add3Btn.setOnAction(e -> addDemoDownloads(table));

        HBox topBar = new HBox(10, add3Btn);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        BorderPane root = new BorderPane(table);
        root.setTop(topBar);
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root, 820, 420));
        stage.setTitle("Interfaz de Descargas (avance)");
        stage.show();
    }

    private void addDemoDownloads(TableView<DownloadTask> table) {
        // Crea 3 “archivos” con tamaños y velocidades distintas
        DownloadItem a = new DownloadItem("video_tutorial.mp4", 50L * 1024 * 1024, 600); // 50MB @ 600KB/s
        DownloadItem b = new DownloadItem("dataset.zip",      120L * 1024 * 1024, 800); // 120MB @ 800KB/s
        DownloadItem c = new DownloadItem("lib.jar",          8L * 1024 * 1024, 400);   // 8MB @ 400KB/s

        DownloadTask t1 = manager.createAndSubmit(a);
        DownloadTask t2 = manager.createAndSubmit(b);
        DownloadTask t3 = manager.createAndSubmit(c);

        table.getItems().addAll(t1, t2, t3);
    }

    @Override
    public void stop() {
        if (manager != null) {
            manager.shutdown();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
