import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class ImageButton extends JButton {

    public ImageButton(String resourcePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url == null) {
                throw new IllegalArgumentException("Image not found: " + resourcePath);
            }

            ImageIcon icon = new ImageIcon(url);
            setIcon(icon);

            // Button styling
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);

            // Auto-size to image
            setBounds(0, 0, icon.getIconWidth(), icon.getIconHeight());

        } catch (Exception e) {
            e.printStackTrace();
            setText("Missing Image");
        }
    }
}
