import javax.swing.*;
import java.awt.*;

public class MainMenu extends JFrame {

    JPanel menuPanel;
    JPanel playPanel;
    JPanel playBot;
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
        createPlayPanel();
        createBotPanel();

        setContentPane(menuPanel);
        setVisible(true);
    }


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
        howtoplay.addActionListener(e -> showHowToPlay());
        menuPanel.add(howtoplay);
    }


    private void createPlayPanel() {
        playPanel = new JPanel() {
            Image bg = new ImageIcon(getClass().getResource("/images/Background.png")).getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };

        playPanel.setLayout(null);

        RoundedButton bot = new RoundedButton("Bot", 25);
        bot.setFont(new Font("Serif", Font.BOLD, 36));
        bot.setForeground(Color.WHITE);
        bot.setBounds(260, 300, 280, 70);
        bot.addActionListener(e -> ShowBot());
        playPanel.add(bot);

        RoundedButton players = new RoundedButton("Players", 25);
        players.setFont(new Font("Serif", Font.BOLD, 36));
        players.setForeground(Color.WHITE);
        players.setBounds(260, 400, 280, 70);
        playPanel.add(players);
    }

    private void createBotPanel() {
        playBot = new JPanel() {
            Image bg = new ImageIcon(getClass().getResource("/images/Background.png")).getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };

        playBot.setLayout(null);

        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(38, 123, 153)); // teal-blue
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
            }
        };
        box.setOpaque(false);
        box.setLayout(null);
        box.setBounds(200, 80, 400, 430);
        playBot.add(box);

        JLabel title = new JLabel("Select difficulty:");
        title.setFont(new Font("Serif", Font.BOLD, 32));
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setBounds(50, 20, 300, 50);
        box.add(title);

        GreenRoundedButton easy = new GreenRoundedButton("Easy", 35);
        easy.setFont(new Font("Serif", Font.BOLD, 30));
        easy.setForeground(Color.WHITE);
        easy.setBounds(60, 100, 280, 70);
        easy.setBackground(new Color(172, 220, 135));  // green
        easy.setContentAreaFilled(false);
        easy.setOpaque(false);
        box.add(easy);

        GreenRoundedButton normal = new GreenRoundedButton("Normal", 35);
        normal.setFont(new Font("Serif", Font.BOLD, 30));
        normal.setForeground(Color.WHITE);
        normal.setBounds(60, 200, 280, 70);
        normal.setBackground(new Color(172, 220, 135));
        normal.setContentAreaFilled(false);
        normal.setOpaque(false);
        box.add(normal);

        GreenRoundedButton hard = new GreenRoundedButton("Hard", 35);
        hard.setFont(new Font("Serif", Font.BOLD, 30));
        hard.setForeground(Color.WHITE);
        hard.setBounds(60, 300, 280, 70);
        hard.setBackground(new Color(172, 220, 135));
        hard.setContentAreaFilled(false);
        hard.setOpaque(false);
        box.add(hard);

    }


    private void createHowToPlayPanel() {
        howToPlayPanel = new JPanel(null);
        howToPlayPanel.setBackground(Color.BLACK);

        images = new ImageIcon[2];
        images[0] = new ImageIcon(getClass().getResource("/images/HowToPlay1.png"));
        images[1] = new ImageIcon(getClass().getResource("/images/HowToPlay2.png"));

        for (int i = 0; i < images.length; i++) {
            Image scaled = images[i].getImage().getScaledInstance(700, 400, Image.SCALE_SMOOTH);
            images[i] = new ImageIcon(scaled);
        }

        imageLabel = new JLabel(images[0]);
        imageLabel.setBounds(50, 50, 700, 400);
        howToPlayPanel.add(imageLabel);

        RoundedButton prev = new RoundedButton("Back", 25);
        prev.setFont(new Font("Serif", Font.BOLD, 32));
        prev.setForeground(Color.WHITE);
        prev.setBounds(50, 500, 160, 60);
        prev.addActionListener(e -> {
            if (currentIndex > 0) {
                currentIndex--;
                imageLabel.setIcon(images[currentIndex]);
            } else showMenu();
        });
        howToPlayPanel.add(prev);

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


    private void showHowToPlay() {
        setContentPane(howToPlayPanel);
        revalidate();
        repaint();
    }

    private void ShowPlay() {
        setContentPane(playPanel);
        revalidate();
        repaint();
    }

    private void ShowBot() {
        setContentPane(playBot);
        revalidate();
        repaint();
    }

    private void showMenu() {
        setContentPane(menuPanel);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        new MainMenu();
    }
}
