package example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Route("")
@PageTitle("Image Gallery")
public class ImageGalleryView extends VerticalLayout {

    private final List<UplodedImage> uploadedImages = new ArrayList<>();
    private final Div gallery = new Div();
    private final UI ui;

    public ImageGalleryView() {
        this.ui = UI.getCurrent();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 title = new H1( " Image Upload & Gallery");

        // Apply modern styling (recommended in recent Vaadin versions)
        title.addClassNames(
                LumoUtility.FontSize.XXLARGE,   // Big, prominent title
                LumoUtility.Margin.NONE,        // Remove default margins
                LumoUtility.TextColor.HEADER    // Optional: semantic header color
        );

        // Optional: Center the title or make it full-width friendly
        setAlignItems(Alignment.CENTER);
        setPadding(true);
        setSpacing(true);

        add(title);

        UploadHandler handler = this::handleUploadedFile;
        Upload upload = new Upload(handler);

        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif", "image/webp");
        upload.setMaxFiles(15);
        upload.setMaxFileSize(15 * 1024 * 1024);

        upload.setDropLabel(new Span("Drag & drop images here or click to upload"));
        upload.setDropLabelIcon(new Icon(VaadinIcon.UPLOAD));
        upload.getElement().executeJs(
                "this.shadowRoot.querySelector('[part=\"drop-label\"]').style.color='#000';" +
                        "this.shadowRoot.querySelector('[part=\"primary-buttons\"]').style.color='#000';"
        );

        gallery.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(260px, 1fr))")
                .set("gap", "20px")
                .set("margin-top", "24px")
                .set("width", "100%")
                .set("font-family", "'Montserrat', sans-serif");

        add(title, upload, gallery);

        createUploadDirIfNotExists();
        loadExistingImages();
        refreshGallery();
    }

    // Create upload directory
    private void createUploadDirIfNotExists() {
        File dir = new File(Application.UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    //  Load existing images
    private void loadExistingImages() {
        File dir = new File(Application.UPLOAD_DIR);
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                uploadedImages.add(new UplodedImage(
                        file.getName(),
                        "/uploaded-images/" + file.getName()
                ));
            }
        }
    }

    //  FIXED Upload Handler
    private void handleUploadedFile(UploadEvent event) {
        String fileName = event.getFileName();

        try (InputStream inputStream = event.getInputStream()) {

            String uniqueFileName = UUID.randomUUID() + "_" + fileName;
            File targetFile = new File(Application.UPLOAD_DIR + uniqueFileName);

            Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            UplodedImage newImage = new UplodedImage(
                    uniqueFileName,
                    "/uploaded-images/" + uniqueFileName
            );

            // FIX: UI thread update
            ui.access(() -> {
                uploadedImages.add(newImage);

                Notification.show("✅ Uploaded: " + fileName,
                        3000, Notification.Position.TOP_END);

                refreshGallery();
            });

        } catch (IOException e) {
            e.printStackTrace();

            ui.access(() -> {
                Notification.show("❌ Upload failed: " + e.getMessage(),
                        5000, Notification.Position.TOP_END);
            });
        }
    }

    private void refreshGallery() {
        gallery.removeAll();

        if (uploadedImages.isEmpty()) {
            gallery.add(new Span("No images uploaded yet."));
            return;
        }

        for (UplodedImage img : uploadedImages) {
            gallery.add(createImageCard(img));
        }
    }

    private VerticalLayout createImageCard(UplodedImage image) {

        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);

        // Card styling
        card.getStyle()
                .set("border-radius", "16px")
                .set("overflow", "hidden")
                .set("background", "#ffffff")
                .set("box-shadow", "0 6px 18px rgba(0,0,0,0.08)")
                .set("transition", "transform 0.2s ease");

        // Image component
        Image imgComponent = new Image(image.url(), image.fileName());
        imgComponent.setWidthFull();
        imgComponent.setHeight("220px");
        imgComponent.getStyle()
                .set("object-fit", "cover")
                .set("cursor", "pointer");
        imgComponent.addClickListener(e -> showLargeImage(image));

        // Delete button (plain, styled with CSS)
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyle()
                .set("background", "#f87171") // red
                .set("color", "#ffffff")
                .set("border", "none")
                .set("border-radius", "4px")
                .set("padding", "4px 6px")
                .set("cursor", "pointer")
                .set("font-size", "0.9rem")
                .set("transition", "background 0.2s ease");
        deleteBtn.addClickListener(e -> deleteImage(image));
        // hover effect
        deleteBtn.getElement().executeJs(
                "this.addEventListener('mouseenter', () => this.style.background='#ef4444');" +
                        "this.addEventListener('mouseleave', () => this.style.background='#f87171');"
        );

        // Image name label
        Span nameLabel = new Span(image.fileName());
        nameLabel.getStyle()
                .set("font-weight", "500")
                .set("font-size", "0.95rem")
                .set("color", "#374151")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap")
                .set("max-width", "180px");

        // Footer layout
        HorizontalLayout footer = new HorizontalLayout(nameLabel, deleteBtn);
        footer.setWidthFull();
        footer.setPadding(true);
        footer.setSpacing(true);
        footer.setAlignItems(Alignment.CENTER);
        footer.getStyle()
                .set("border-top", "1px solid #e5e7eb")
                .set("background", "#f9fafb");

        // Add components to card
        card.add(imgComponent, footer);

        // Optional hover effect on card
        card.getElement().getStyle().set("transition", "transform 0.2s");
        card.getElement().addEventListener("mouseenter", e ->
                card.getStyle().set("transform", "scale(1.03)")
        );
        card.getElement().addEventListener("mouseleave", e ->
                card.getStyle().set("transform", "scale(1)")
        );

        return card;
    }

    private void showLargeImage(UplodedImage image) {
        Dialog dialog = new Dialog();
        dialog.setWidth("80%");
        dialog.setHeight("80%");

        Image large = new Image(image.url(), "");
        large.setSizeFull();
        large.getStyle().set("object-fit", "contain");

        dialog.add(large);
        dialog.open();
    }

    private void deleteImage(UplodedImage image) {
        File file = new File(Application.UPLOAD_DIR + image.fileName());

        if (file.exists()) {
            file.delete();
        }

        uploadedImages.remove(image);
        refreshGallery();

        Notification.show("🗑️ Deleted", 2000,
                Notification.Position.TOP_END);
    }
}
