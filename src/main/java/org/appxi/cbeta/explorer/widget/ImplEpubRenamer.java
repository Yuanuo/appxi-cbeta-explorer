package org.appxi.cbeta.explorer.widget;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.holder.IntHolder;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.tome.cbeta.BookTree;
import org.appxi.tome.cbeta.BookTreeMode;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.util.DigestHelper;
import org.appxi.util.FileHelper;
import org.appxi.util.StringHelper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

class ImplEpubRenamer extends Widget {
    ImplEpubRenamer(WorkbenchViewController controller) {
        super(controller);
    }

    @Override
    String getName() {
        return "CBETA EPUB 重构";
    }

    @Override
    Node getViewport() {
        final Label info = new Label("由于CBETA提供的EPUB电子书使用图书ID作为文件名，若是人来查找需要的图书电子档，实在不太方便。\n此工具用于将所有EPUB文件名称规范化并且导出到合适的目录结构中，以解决此问题。\n");
        info.setWrapText(true);

        Label sourcePathLabel = new Label("输入（ZIP压缩包）");
        sourcePathLabel.setStyle("-fx-padding: 1em 0;");

        TextField sourcePathField = new TextField();
        sourcePathField.setEditable(false);
        HBox.setHgrow(sourcePathField, Priority.ALWAYS);
        Button sourcePathBtn = new Button("...");
        RawHolder<File> sourceFileHold = new RawHolder<>();
        sourcePathBtn.setOnAction(evt -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("zip", "*.zip"));
            sourceFileHold.value = fc.showOpenDialog(this.controller.getPrimaryStage());
            if (null != sourceFileHold.value) {
                sourcePathField.setText(sourceFileHold.value.getAbsolutePath());
            }
        });
        HBox sourcePathHBox = new HBox(sourcePathField, sourcePathBtn);

        Label targetTypeLabel = new Label("使用目录模式");
        targetTypeLabel.setStyle("-fx-padding: 1em 0;");

        ComboBox<String> targetTypeBox = new ComboBox<>();
        targetTypeBox.getItems().addAll(BookTreeMode.catalog.name(), BookTreeMode.simple.name(), BookTreeMode.advance.name());
        targetTypeBox.getSelectionModel().select(0);

        Label targetPathLabel = new Label("输出到文件夹");
        targetPathLabel.setStyle("-fx-padding: 1em 0;");

        TextField targetPathField = new TextField();
        targetPathField.setEditable(false);
        HBox.setHgrow(targetPathField, Priority.ALWAYS);
        Button targetPathBtn = new Button("...");
        targetPathBtn.setOnAction(evt -> {
            DirectoryChooser dc = new DirectoryChooser();
            if (null != sourceFileHold.value)
                dc.setInitialDirectory(sourceFileHold.value.getParentFile());
            File selDir = dc.showDialog(this.controller.getPrimaryStage());
            if (null != selDir)
                targetPathField.setText(selDir.getAbsolutePath());
        });
        HBox targetPathHBox = new HBox(targetPathField, targetPathBtn);

        Button applyBtn = new Button("Apply");
        applyBtn.setOnAction(event -> {
            String result = handleApplyAction(sourcePathField.getText(),
                    targetTypeBox.getSelectionModel().getSelectedItem(), targetPathField.getText());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText(result);
            FxHelper.withTheme(this.controller.getApplication(), alert).show();
        });
        HBox applyBtnHBox = new HBox(applyBtn);
        applyBtnHBox.setAlignment(Pos.CENTER);
        applyBtnHBox.setStyle("-fx-padding: 1em 0;");

        return new VBox(info,
                sourcePathLabel, sourcePathHBox,
                targetTypeLabel, targetTypeBox,
                targetPathLabel, targetPathHBox,
                applyBtnHBox);
    }

    private String handleApplyAction(String sourcePath, String bookTreeMode, String targetPath) {
        if (sourcePath.isBlank() || bookTreeMode.isBlank() || targetPath.isBlank())
            return "未指定 输入文件 或 目录模式 或 输出目录";

        Path source = Path.of(sourcePath);
        if (FileHelper.notExists(source))
            return "输入文件不正确";
        Path target = Path.of(targetPath);
        FileHelper.makeDirs(target);
        if (!Files.isWritable(target))
            return "输出目录不正确";
        BookTreeMode mode = BookTreeMode.valueOf(bookTreeMode);
        final BookTree.WithMap bookTree = new BookTree.WithMap(BookList.books, mode);
        bookTree.getDataTree();

        IntHolder totalNum = new IntHolder(0);
        IntHolder savedNum = new IntHolder(0);
        try (ZipFile zipFile = new ZipFile(source.toFile())) {
            zipFile.stream()//
                    .filter(entry -> entry.getName().endsWith(".epub"))//
                    .forEach(entry -> {
                        totalNum.value++;
                        String bookPath = entry.getName().replace("\\", "/");
                        bookPath = bookPath.substring(bookPath.indexOf("/") + 1);

                        String bookName;
                        String bookId = bookPath.substring(bookPath.lastIndexOf("/") + 1);
                        bookPath = bookPath.substring(bookPath.lastIndexOf("/") + 1);
                        bookId = bookId.substring(0, bookId.length() - 5);// remove ".epub"
                        org.appxi.util.ext.Node<CbetaBook> bookNode = bookTree.getDataMap().get(bookId);
                        if (null == bookNode)
                            bookNode = bookTree.getDataMap().get(bookId + "a");
                        if (null != bookNode) {
                            bookPath = bookNode.parents().stream()//
                                    .filter(n -> n.value != null)//
                                    .map(n -> n.value.title).collect(Collectors.joining("/"));
                            bookPath = bookPath.replaceAll("[<>:\"|?*]", "");
                            // build name
                            bookName = bookNode.value.title;
                            if (StringHelper.isNotBlank(bookNode.value.authorInfo))
                                bookName = bookName + " - " + bookNode.value.authorInfo.replace(" ", "");
                            bookName = bookName.replaceAll("[<>:\"|?*]", "");
                            if (bookName.length() > 200)
                                bookName = bookName.substring(0, 200) + ",etc";
                        } else {
                            bookName = bookId;
                        }
                        try {
                            Path targetFile = target.resolve(bookPath).resolve(bookName + ".epub");
                            if (FileHelper.exists(targetFile))
                                targetFile = target.resolve(bookPath).resolve(bookName + "(" + bookId + ")" + ".epub");
                            if (FileHelper.exists(targetFile))
                                targetFile = target.resolve(bookPath).resolve(bookName + "(" + DigestHelper.uid() + ")" + ".epub");
                            FileHelper.makeParents(targetFile);
                            Files.copy(zipFile.getInputStream(entry), targetFile);
                            savedNum.value++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return StringHelper.concat("操作完成。导出 / 总数：", savedNum.value, " / ", totalNum.value);
    }

    @Override
    void onViewportShowing(boolean firstTime) {

    }
}
