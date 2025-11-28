import javax.swing.*;
import java.awt.*;

public class MainMenu extends JFrame {

    JPanel menuPanel;
    JPanel howToPlayPanel;
    JLabel imageLabel;
    ImageIcon[] images;
    int currentIndex = 0;

    public MainMenu() {
        setTitle("Holen Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        createMenuPanel();
        createHowToPlayPanel();

        setContentPane(menuPanel);
        setVisible(true);
    }

    // ------------------- Main Menu Panel -------------------
    private void createMenuPanel() {
        menuPanel = new JPanel() {
            Image bg = new ImageIcon(getClass().getResource("/images/Background.png")).getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };

        menuPanel.setLayout(null);

        RoundedButton play = new RoundedButton("Play", 25);
        play.setFont(new Font("Serif", Font.BOLD, 36));
        play.setForeground(Color.WHITE);
        play.setBounds(260, 300, 280, 70);
        play.addActionListener(e -> ShowPlay());
        menuPanel.add(play);

        RoundedButton howtoplay = new RoundedButton("How to Play", 25);
        howtoplay.setFont(new Font("Serif", Font.BOLD, 36));
        howtoplay.setForeground(Color.WHITE);
        howtoplay.setBounds(260, 400, 280, 70);
        menuPanel.add(howtoplay);

        howtoplay.addActionListener(e -> showHowToPlay());
    }
//
//    private void PlayPanel(){
//
//    }

    private void createHowToPlayPanel() {
        howToPlayPanel = new JPanel();
        howToPlayPanel.setLayout(null);
        howToPlayPanel.setBackground(Color.BLACK);

        images = new ImageIcon[2];
        images[0] = new ImageIcon(getClass().getResource("/images/HowToPlay1.png"));
        images[1] = new ImageIcon(getClass().getResource("/images/HowToPlay2.png"));

        for (int i = 0; i < images.length; i++) {
            Image scaled = images[i].getImage().getScaledInstance(700, 400, Image.SCALE_SMOOTH);
            images[i] = new ImageIcon(scaled);
        }

        imageLabel = new JLabel(images[0]);
        imageLabel.setBounds(50, 80, 700, 400);
        howToPlayPanel.add(imageLabel);

        RoundedButton prev = new RoundedButton("Back", 25);
        prev.setFont(new Font("Serif", Font.BOLD, 32));
        prev.setForeground(Color.WHITE);
        prev.setBounds(50, 500, 160, 60);
        prev.addActionListener(e -> {
            if (currentIndex > 0) {
                currentIndex--;
                imageLabel.setIcon(images[currentIndex]);
            } else {
                showMenu();
            }
        });
        howToPlayPanel.add(prev);

        // Next button
        RoundedButton next = new RoundedButton("Next", 25);
        next.setFont(new Font("Serif", Font.BOLD, 32));
        next.setForeground(Color.WHITE);
        next.setBounds(600, 500, 160, 60);
        next.addActionListener(e -> {
            if (currentIndex < images.length - 1) {
                currentIndex++;
                imageLabel.setIcon(images[currentIndex]);
            }
        });
        howToPlayPanel.add(next);
    }

    // ------------------- Panel Switch Methods -------------------
    private void showHowToPlay() {
        setContentPane(howToPlayPanel);
        revalidate();
        repaint();
        currentIndex = 0;
        imageLabel.setIcon(images[currentIndex]);
    }

    private void ShowPlay(){
//        setContentPane(playPanel);
        revalidate();
        repaint();
    }

    private void showMenu() {
        setContentPane(menuPanel);
        revalidate();
        repaint();
    }

    // ------------------- Main Method -------------------
    public static void main(String[] args) {
        new MainMenu();
    }
}
