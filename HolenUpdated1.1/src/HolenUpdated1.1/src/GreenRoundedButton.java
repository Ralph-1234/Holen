import javax.swing.*;
import java.awt.*;

public class GreenRoundedButton extends JButton {

    private int radius;

    public GreenRoundedButton(String text, int radius) {
        super(text);
        this.radius = radius;

        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(172, 220, 135));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

        super.paintComponent(g);
        g2.dispose();
    }
}

