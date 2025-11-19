import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HolenGameFull.java
 * Single-file, fully playable test build (PvP / PvB)
 *
 * - One circle field
 * - Multi-player & multi-bot support (setup)
 * - Drag-to-shoot with force %, projected path while dragging
 * - No wall bounce (marbles that go far outside are removed)
 * - Throwables decrement immediately on shoot; replacement spawned if player still has throwables
 * - Elastic-ish collisions, scoring when neutral marbles leave the circle
 * - Turn order sequential across players & bots
 * - Start/Back/How-to UI restored and aligned
 */
class HolenGame extends JFrame {

    CardLayout card = new CardLayout();
    JPanel cards = new JPanel(card);

    MenuScreen menuScreen = new MenuScreen();
    DifficultyScreen difficultyScreen = new DifficultyScreen();
    SetupScreen setupScreen = new SetupScreen();
    GameScreen gameScreen = new GameScreen();
    ResultScreen resultScreen = new ResultScreen();

    // settings
    int throwablePerPlayer = 5;
    int marblesInCircle = 8;
    int numPlayers = 2;   // used when PvP
    int numBots = 1;      // used when PvB
    BotDifficulty difficulty = BotDifficulty.NORMAL;
    GameMode mode = GameMode.PVP;

    public HolenGame() {
        setTitle("Holen Game");
        setSize(1024, 720); // slightly bigger than default, not true fullscreen
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cards.add(menuScreen, "menu");
        cards.add(difficultyScreen, "difficulty");
        cards.add(setupScreen, "setup");
        cards.add(gameScreen, "game");
        cards.add(resultScreen, "result");

        add(cards);
        card.show(cards, "menu");
        setVisible(true);
    }

    enum BotDifficulty { EASY, NORMAL, HARD }
    enum GameMode { PVP, PVB }

    // ---------- Model classes ----------

    class Player {
        String name;
        Color color;
        int throwables;
        int collected;
        public Player(String name, Color color, int throwables) {
            this.name = name; this.color = color; this.throwables = throwables; this.collected = 0;
        }
        boolean hasMarbles() { return throwables > 0; }
        void useThrowable() { if (throwables > 0) throwables--; }
        void collect() { collected++; }
    }

    class BotPlayer extends Player {
        double accuracy;
        int reactionTime;
        public BotPlayer(String name, Color color, int throwables, BotDifficulty diff) {
            super(name, color, throwables);
            switch(diff){
                case EASY: accuracy = 0.4; reactionTime = 900; break;
                case NORMAL: accuracy = 0.65; reactionTime = 650; break;
                case HARD: accuracy = 0.85; reactionTime = 420; break;
            }
        }
    }

    class Marble {
        double x, y, vx = 0, vy = 0;
        final int R = 12;
        boolean insideCircle;
        Player owner;           // owner indicates initial owner (player marble) or null for neutral
        Player lastTouchedBy;   // who last moved/touched it (for scoring)

        Marble(double x, double y, Player owner, boolean inside) {
            this.x = x; this.y = y; this.owner = owner; this.insideCircle = inside;
            this.lastTouchedBy = null;
        }
        void update(double dt) {
            x += vx * dt; y += vy * dt;
            vx *= 0.995; vy *= 0.995;
            if (Math.hypot(vx, vy) < 0.03) { vx = 0; vy = 0; }
        }
        boolean moving() { return Math.hypot(vx, vy) > 0.2; }
    }

    class CircleField {
        double cx, cy, r;
        CircleField(double cx, double cy, double r) { this.cx = cx; this.cy = cy; this.r = r; }
        boolean inside(double x, double y) { return Math.hypot(x - cx, y - cy) <= r; }
    }

    class Game {
        List<Player> players = new ArrayList<>();
        List<Marble> marbles = new ArrayList<>();
        CircleField field;
        int turnIndex = 0;
        Map<Player, Point> spawn = new HashMap<>();

        Game(GameMode gm, int numPlayersArg, int numBotsArg, int throwables, int inside, BotDifficulty diff) {
            field = new CircleField(512, 330, 140);
            Random rng = new Random();

            if (gm == GameMode.PVP) {
                for (int i = 0; i < Math.max(2, numPlayersArg); i++) {
                    Color col = new Color(80 + rng.nextInt(120), 80 + rng.nextInt(120), 80 + rng.nextInt(120));
                    players.add(new Player("P" + (i + 1), col, throwables));
                }
            } else {
                // Player first, then bots
                players.add(new Player("YOU", Color.CYAN, throwables));
                for (int i = 0; i < Math.max(1, numBotsArg); i++) {
                    players.add(new BotPlayer("BOT" + (i + 1), Color.RED, throwables, diff));
                }
            }

            // neutral marbles
            for (int i = 0; i < inside; i++) {
                double a = rng.nextDouble() * 2 * Math.PI;
                double d = rng.nextDouble() * (field.r - 20);
                marbles.add(new Marble(field.cx + Math.cos(a) * d, field.cy + Math.sin(a) * d, null, true));
            }

            // spawn positions horizontally below the circle
            int spacing = 80;
            int total = players.size();
            int startX = (int) field.cx - spacing * (total - 1) / 2;
            int y = (int) field.cy + 260;
            for (Player p : players) {
                Marble m = new Marble(startX, y, p, false);
                marbles.add(m);
                spawn.put(p, new Point(startX, y));
                startX += spacing;
            }

            turnIndex = 0;
        }

        Player getCurrentPlayer() { return players.get(turnIndex); }
        void nextTurn() { turnIndex = (turnIndex + 1) % players.size(); }
        boolean allGone() { return players.stream().allMatch(p -> !p.hasMarbles()); }
    }

    // ---------- UI helpers ----------
    JButton uiButton(String text, int w, int h) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(w, h));
        b.setBackground(Color.WHITE);
        b.setForeground(Color.BLACK);
        b.setFont(new Font("Arial", Font.BOLD, 22));
        b.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
        return b;
    }
    JLabel uiTitle(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 44));
        l.setForeground(Color.WHITE);
        return l;
    }

    // ---------- Screens ----------

    class MenuScreen extends JPanel {

        private Image bg;
        private Image how1;

        MenuScreen() {
            bg = new ImageIcon(getClass().getResource("/images/Background.png")).getImage();
            setLayout(null);

            JLabel title = uiTitle("HOLEN");
            title.setFont(new Font("Arial", Font.BOLD, 72));
            title.setForeground(Color.BLACK);
            title.setBounds(0, 20, 1024, 100);
            add(title);

            int btnW = 420, btnH = 60;
            int centerX = (1024 - btnW) / 2;
            int startY = 160;
            int spacing = 86;

            ImageIcon PVPIcon = new ImageIcon(getClass().getResource("/images/PlayerVSPlayers.png"));
            JButton pvp = new JButton(PVPIcon);
            pvp.setBounds(362, 150, 300, 60);
            pvp.addActionListener(e -> { mode = GameMode.PVP; setupScreen.updateVisibleOptions(); card.show(cards, "setup"); });
            add(pvp);

            ImageIcon PVBIcon = new ImageIcon(getClass().getResource("/images/PlayersVSBot.png"));
            JButton pvb = new JButton(PVBIcon);
            pvb.setBounds(362, 240, 300, 60);
            pvb.addActionListener(e -> { mode = GameMode.PVB; card.show(cards, "difficulty"); });
            add(pvb);

//            JButton how = uiButton("HOW TO PLAY", btnW, btnH);
//            how.setBounds(centerX, startY + spacing * 2, btnW, btnH);
//            how.addActionListener(e -> {
//                String msg = "HOW TO PLAY:\n\n" +
//                        "1. Choose mode (PvP or PvB) and setup.\n" +
//                        "2. Each player has throwable marbles shown below the field.\n" +
//                        "3. Click a throw marble, drag away to set force, release to shoot.\n" +
//                        "4. Knock neutral marbles out of the circle to score.\n" +
//                        "5. Throwables decrease when a marble is shot; replacements appear while supply remains.\n\n" +
//                        "Tip: drag longer for more force. Force is shown as % while dragging.\n" +
//                        "A faint projected path is shown while dragging.";
//                String html = "<html><div style='font-size:14px; width:480px;'>" + msg.replace("\n", "<br>") + "</div></html>";
//                JOptionPane.showMessageDialog(this, html, "How To Play", JOptionPane.PLAIN_MESSAGE);
//            });
//
//           setLayout(null);
            ImageIcon howIcon = new ImageIcon(getClass().getResource("/images/How.png"));
            JButton how = new JButton(howIcon);
            how.setBounds(362, 340, 300, 60);
            how.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ImageIcon howToPlayIcon2 = new ImageIcon(getClass().getResource("/images/HowToPlay2.png"));
                    ImageIcon howToPlayIcon = new ImageIcon(getClass().getResource("/images/HowToPlay1.png"));
                    JLabel label1 = new JLabel(howToPlayIcon2);
                    JFrame frame1 = new JFrame("How to Play");
                    frame1.add(label1);
                    frame1.pack();
                    frame1.setVisible(true);
                    frame1.setBounds(50, 20, 1200, 1100);
                    JLabel label = new JLabel(howToPlayIcon);
                    JFrame frame = new JFrame("How to Play");
                    frame.add(label);
                    frame.pack();
                    frame.setVisible(true);
                    frame.setBounds(50, 20, 1200, 1100);
                }
            });

            add(how);

            JButton exit = uiButton("EXIT", btnW, btnH);
            exit.setBounds(centerX, startY + spacing * 3, btnW, btnH);
            exit.addActionListener(e -> System.exit(0));
            add(exit);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    class DifficultyScreen extends JPanel {
        private Image setImg;
        DifficultyScreen() {
            setImg = new ImageIcon(getClass().getResource("/images/SetDifficulty.png")).getImage();
            setLayout(null);
            JLabel title = uiTitle("Select Bot Difficulty");
            title.setBounds(0, 40, 1024, 60);
            add(title);
//
//            JButton easy = uiButton("Easy", 300, 60);
//            easy.setBounds(362, 150, 300, 60);
//            easy.addActionListener(e -> { difficulty = BotDifficulty.EASY; card.show(cards, "setup"); });
//            add(easy);

            ImageIcon playIcon = new ImageIcon(getClass().getResource("/images/Easy.png"));
            JButton easy = new JButton(playIcon);
            easy.setBounds(362, 150, 300, 60);
            easy.setBorderPainted(false);
            easy.setContentAreaFilled(false);  // removes grey button background
            easy.setFocusPainted(false);       // removes focus outline
            easy.addActionListener(e -> {
                difficulty = BotDifficulty.EASY;
                card.show(cards, "setup");
            });
            add(easy);


            ImageIcon mediumIcon = new ImageIcon(getClass().getResource("/images/Normal.png"));
            JButton norm = new JButton(mediumIcon);
            norm.setBounds(362, 240, 300, 60);
            norm.addActionListener(e -> {
                difficulty = BotDifficulty.NORMAL;
                card.show(cards, "setup");
            });
            add(norm);

            ImageIcon hardIcon = new ImageIcon(getClass().getResource("/images/Hard.png"));
            JButton hard = new JButton(hardIcon);
            hard.setBounds(362, 330, 300, 60);
            hard.addActionListener(e -> { difficulty = BotDifficulty.HARD; card.show(cards, "setup"); });
            add(hard);

            ImageIcon backIcon = new ImageIcon(getClass().getResource("/images/Back.png"));
            JButton back = new JButton(backIcon);
            back.setBounds(412, 430, 200, 50);
            back.addActionListener(e -> card.show(cards, "menu"));
            easy.setBorderPainted(false);
            easy.setContentAreaFilled(true);
            easy.setFocusPainted(true);
            add(back);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(setImg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    class SetupScreen extends JPanel {
        JLabel lThrow, lInside, lNumPlayers, lNumBots;
        JButton pMinus, pPlus, bMinus, bPlus;

        private Image GameSet;
        SetupScreen() {
            GameSet = new ImageIcon(getClass().getResource("/images/GameSetUp.png")).getImage();
            setLayout(null);

            JLabel title = uiTitle("Game Setup");
            title.setBounds(0, 20, 1024, 60);
            title.setForeground(Color.BLACK);
            add(title);

            lThrow = new JLabel("Throwables: " + throwablePerPlayer, SwingConstants.CENTER);
            lThrow.setBounds(200, 110, 560, 30);
            lThrow.setForeground(Color.BLACK);
            add(lThrow);

            JButton tMinus = uiButton("-", 70, 40);
            tMinus.setBounds(200, 150, 70, 40);
            tMinus.addActionListener(e -> { if (throwablePerPlayer > 1) throwablePerPlayer--; lThrow.setText("Throwables: " + throwablePerPlayer); });
            add(tMinus);

            JButton tPlus = uiButton("+", 70, 40);
            tPlus.setBounds(690, 150, 70, 40);
            tPlus.addActionListener(e -> { throwablePerPlayer++; lThrow.setText("Throwables: " + throwablePerPlayer); });
            add(tPlus);

            lInside = new JLabel("Marbles in circle: " + marblesInCircle, SwingConstants.CENTER);
            lInside.setBounds(200, 210, 560, 30);
            lInside.setForeground(Color.BLACK);
            add(lInside);

            JButton iMinus = uiButton("-", 70, 40);
            iMinus.setBounds(200, 250, 70, 40);
            iMinus.addActionListener(e -> { if (marblesInCircle > 0) marblesInCircle--; lInside.setText("Marbles in circle: " + marblesInCircle); });
            add(iMinus);

            JButton iPlus = uiButton("+", 70, 40);
            iPlus.setBounds(690, 250, 70, 40);
            iPlus.addActionListener(e -> { marblesInCircle++; lInside.setText("Marbles in circle: " + marblesInCircle); });
            add(iPlus);

            // number of players (PvP)
            lNumPlayers = new JLabel("Number of players: " + numPlayers, SwingConstants.CENTER);
            lNumPlayers.setBounds(200, 310, 560, 30);
            lNumPlayers.setForeground(Color.BLACK);
            add(lNumPlayers);

            pMinus = uiButton("-", 70, 40);
            pMinus.setBounds(200, 350, 70, 40);
            pMinus.addActionListener(e -> { if (numPlayers > 2) numPlayers--; lNumPlayers.setText("Number of players: " + numPlayers); });
            add(pMinus);

            pPlus = uiButton("+", 70, 40);
            pPlus.setBounds(690, 350, 70, 40);
            pPlus.addActionListener(e -> { if (numPlayers < 6) numPlayers++; lNumPlayers.setText("Number of players: " + numPlayers); });
            add(pPlus);

            // number of bots (PvB) - same placement as number players (Option A)
            lNumBots = new JLabel("Number of bots: " + numBots, SwingConstants.CENTER);
            lNumBots.setBounds(200, 310, 560, 30);
            lNumBots.setForeground(Color.WHITE);
            add(lNumBots);

            bMinus = uiButton("-", 70, 40);
            bMinus.setBounds(200, 350, 70, 40);
            bMinus.addActionListener(e -> { if (numBots > 1) numBots--; lNumBots.setText("Number of bots: " + numBots); });
            add(bMinus);

            bPlus = uiButton("+", 70, 40);
            bPlus.setBounds(690, 350, 70, 40);
            bPlus.addActionListener(e -> { if (numBots < 6) numBots++; lNumBots.setText("Number of bots: " + numBots); });
            add(bPlus);

            // start & back (moved higher)
            JButton start = uiButton("START GAME", 320, 56);
            start.setBounds(352, 460, 320, 56);
            start.addActionListener(e -> {
                Game g;
                if (mode == GameMode.PVB) g = new Game(mode, 1, numBots, throwablePerPlayer, marblesInCircle, difficulty);
                else g = new Game(mode, numPlayers, 0, throwablePerPlayer, marblesInCircle, difficulty);
                gameScreen.begin(g);
                card.show(cards, "game");
            });
            add(start);

            JButton back = uiButton("BACK", 200, 50);
            back.setBounds(412, 540, 200, 50);
            back.addActionListener(e -> card.show(cards, "menu"));
            add(back);

            updateVisibleOptions();
        }

        void updateVisibleOptions() {
            boolean pvpVisible = mode == GameMode.PVP;
            lNumPlayers.setVisible(pvpVisible);
            pMinus.setVisible(pvpVisible);
            pPlus.setVisible(pvpVisible);

            boolean pvbVisible = mode == GameMode.PVB;
            lNumBots.setVisible(pvbVisible);
            bMinus.setVisible(pvbVisible);
            bPlus.setVisible(pvbVisible);
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(GameSet, 0, 0, getWidth(), getHeight(), this);
        }
    }

    class ResultScreen extends JPanel {
        JLabel title = new JLabel("", SwingConstants.CENTER);
        JLabel scores = new JLabel("", SwingConstants.CENTER);
        JButton again, menu;

        ResultScreen() {
            setBackground(new Color(25, 25, 25));
            setLayout(null);
            title.setFont(new Font("Arial", Font.BOLD, 40));
            title.setForeground(Color.WHITE);
            title.setBounds(0, 40, 1024, 60);
            add(title);
            scores.setFont(new Font("Arial", Font.PLAIN, 24));
            scores.setForeground(Color.WHITE);
            scores.setBounds(0, 120, 1024, 300);
            add(scores);
            again = uiButton("PLAY AGAIN", 300, 56);
            again.setBounds(362, 420, 300, 56);
            again.addActionListener(e -> card.show(cards, "setup"));
            add(again);
            menu = uiButton("MAIN MENU", 300, 56);
            menu.setBounds(362, 500, 300, 56);
            menu.addActionListener(e -> card.show(cards, "menu"));
            add(menu);
        }

        void show(List<Player> players) {
            Player winner = players.stream().max(Comparator.comparingInt(p -> p.collected)).orElse(players.get(0));
            title.setText(winner.name + " WINS!");
            StringBuilder sb = new StringBuilder("<html><center>Scores<br>");
            for (Player p : players) sb.append(p.name).append(": ").append(p.collected).append("<br>");
            sb.append("</center></html>");
            scores.setText(sb.toString());
        }
    }

    // ---------- Game (Play) screen ----------
    class GameScreen extends JPanel implements MouseListener, MouseMotionListener {
        Game game;
        javax.swing.Timer loop, botTimer;
        Marble selected;
        Point dragStart, dragNow;
        boolean turnShot = false;
        final double MAX_FORCE = 24.0; // longer drag required
        Random rng = new Random();

        GameScreen() {
            setBackground(new Color(20, 20, 20));
            addMouseListener(this);
            addMouseMotionListener(this);
            loop = new javax.swing.Timer(17, e -> updateGame());
        }

        void begin(Game g) {
            this.game = g;
            selected = null; dragStart = dragNow = null; turnShot = false;
            if (botTimer != null) botTimer.stop();
            loop.start();
            // if starting player is bot, schedule bot
            if (game.getCurrentPlayer() instanceof BotPlayer) scheduleBot();
        }

        void stopAllTimers() { loop.stop(); if (botTimer != null) botTimer.stop(); }

        /** Helper: returns true if any marble in the current game is moving. */
        private boolean moving() {
            if (game == null) return false;
            for (Marble m : game.marbles) if (m.moving()) return true;
            return false;
        }

        void updateGame() {
            if (game == null) return;

            // physics update
            for (Marble m : new ArrayList<>(game.marbles)) m.update(1.0);

            // collisions (pairwise elastic-ish)
            int n = game.marbles.size();
            for (int i = 0; i < n; i++) {
                Marble a = game.marbles.get(i);
                for (int j = i + 1; j < n; j++) {
                    Marble b = game.marbles.get(j);
                    double dx = b.x - a.x, dy = b.y - a.y;
                    double dist = Math.hypot(dx, dy);
                    double minDist = a.R + b.R;
                    if (dist < 0.001) dist = 0.001;
                    if (dist < minDist) {
                        double overlap = minDist - dist;
                        double nx = dx / dist, ny = dy / dist;
                        b.x += nx * (overlap / 2.0); b.y += ny * (overlap / 2.0);
                        a.x -= nx * (overlap / 2.0); a.y -= ny * (overlap / 2.0);

                        double rvx = b.vx - a.vx, rvy = b.vy - a.vy;
                        double rel = rvx * nx + rvy * ny;
                        if (rel > 0) continue;
                        double e = 0.9; // restitution
                        double imp = -(1 + e) * rel / 2.0;
                        a.vx -= imp * nx; a.vy -= imp * ny;
                        b.vx += imp * nx; b.vy += imp * ny;

                        if (a.lastTouchedBy != null) b.lastTouchedBy = a.lastTouchedBy;
                        else if (a.owner != null) b.lastTouchedBy = a.owner;
                        if (b.lastTouchedBy != null) a.lastTouchedBy = b.lastTouchedBy;
                    }
                }
            }

            // remove marbles that go far outside (no wall bounce)
            List<Marble> toRemove = new ArrayList<>();
            for (Marble m : new ArrayList<>(game.marbles)) {
                if (m.x < -120 || m.x > getWidth() + 120 || m.y < -120 || m.y > getHeight() + 120) {
                    toRemove.add(m);
                }
            }
            for (Marble m : toRemove) {
                Player owner = m.owner;
                game.marbles.remove(m);
                // spawn replacement if owner still has throwables
                if (owner != null && owner.hasMarbles()) {
                    Point sp = game.spawn.get(owner);
                    if (sp != null) {
                        Marble repl = new Marble(sp.x, sp.y, owner, false);
                        game.marbles.add(repl);
                    }
                }
            }

            // scoring for marbles leaving circle
            for (Marble m : new ArrayList<>(game.marbles)) {
                if (m.insideCircle && !game.field.inside(m.x, m.y)) {
                    m.insideCircle = false;
                    if (m.lastTouchedBy != null) m.lastTouchedBy.collect();
                }
            }

            boolean anyMoving = moving();

            if (!anyMoving && turnShot) {
                turnShot = false;
                if (game.allGone()) {
                    stopAllTimers();
                    resultScreen.show(game.players);
                    card.show(cards, "result");
                    return;
                } else {
                    // next player's turn sequentially
                    game.nextTurn();
                    // schedule bot if it's bot's turn
                    if (game.getCurrentPlayer() instanceof BotPlayer) scheduleBot();
                }
            }

            repaint();
        }

        void scheduleBot() {
            Player cp = game.getCurrentPlayer();
            if (!(cp instanceof BotPlayer)) return;
            BotPlayer bot = (BotPlayer) cp;
            int delay = bot.reactionTime;
            botTimer = new javax.swing.Timer(delay, e -> {
                performBotShot();
                turnShot = true;
                botTimer.stop();
            });
            botTimer.setRepeats(false);
            botTimer.start();
        }

        void performBotShot() {
            Player p = game.getCurrentPlayer();
            if (!(p instanceof BotPlayer)) return;
            BotPlayer bot = (BotPlayer) p;

            List<Marble> choices = game.marbles.stream().filter(m -> m.owner == bot && !m.moving()).collect(Collectors.toList());
            if (choices.isEmpty()) return;
            Marble chosen = choices.get(rng.nextInt(choices.size()));

            // aim roughly toward center, with inaccuracy
            double aim = Math.atan2(game.field.cy - chosen.y, game.field.cx - chosen.x);
            double angle = aim + (rng.nextDouble() - 0.5) * (1.0 - bot.accuracy);
            double speed = 3 + rng.nextDouble() * 7;
            speed = Math.min(speed, MAX_FORCE);
            chosen.vx = speed * Math.cos(angle);
            chosen.vy = speed * Math.sin(angle);
            chosen.lastTouchedBy = bot;
            bot.useThrowable();
        }

        /**
         * Project estimated path for a shooter given initial vx/vy.
         * Stops when path gets far outside or when it intersects any marble (distance threshold).
         */
        List<Point2D> projectPath(double sx, double sy, double initVx, double initVy, Marble self) {
            List<Point2D> pts = new ArrayList<>();
            double px = sx, py = sy;
            double vx = initVx, vy = initVy;
            pts.add(new Point2D.Double(px, py));
            double dt = 0.8;
            for (int step = 0; step < 300; step++) {
                px += vx * dt; py += vy * dt;
                vx *= 0.995; vy *= 0.995;
                pts.add(new Point2D.Double(px, py));
                // collision with other marbles: ignore very near the shooter initial marble (self)
                for (Marble m : game.marbles) {
                    if (m == self) continue; // skip self
                    double d = Math.hypot(px - m.x, py - m.y);
                    if (d <= m.R * 2.0) {
                        return pts;
                    }
                }
                if (px < -120 || px > getWidth() + 120 || py < -120 || py > getHeight() + 120) return pts;
            }
            return pts;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (game == null) return;
            Graphics2D g2 = (Graphics2D) g;

            // draw circle field
            g2.setColor(new Color(80, 80, 80));
            g2.fillOval((int) (game.field.cx - game.field.r), (int) (game.field.cy - game.field.r),
                    (int) (game.field.r * 2), (int) (game.field.r * 2));
            g2.setColor(Color.WHITE);
            g2.drawOval((int) (game.field.cx - game.field.r), (int) (game.field.cy - game.field.r),
                    (int) (game.field.r * 2), (int) (game.field.r * 2));

            // draw marbles
            for (Marble m : game.marbles) {
                g2.setColor(m.owner != null ? m.owner.color : Color.YELLOW);
                g2.fillOval((int) (m.x - m.R), (int) (m.y - m.R), 2 * m.R, 2 * m.R);
                g2.setColor(Color.BLACK);
                g2.drawOval((int) (m.x - m.R), (int) (m.y - m.R), 2 * m.R, 2 * m.R);
            }

            // projected path while dragging
            if (selected != null && dragStart != null && dragNow != null) {
                double dx = dragStart.x - dragNow.x;
                double dy = dragStart.y - dragNow.y;
                double raw = Math.hypot(dx, dy);
                double speed = Math.min(MAX_FORCE, raw / 8.0);
                double angle = Math.atan2(dy, dx);
                double ivx = speed * Math.cos(angle), ivy = speed * Math.sin(angle);
                List<Point2D> path = projectPath(selected.x, selected.y, ivx, ivy, selected);
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                g2.setColor(Color.WHITE);
                for (int i = 1; i < path.size(); i++) {
                    Point2D a = path.get(i - 1), b = path.get(i);
                    g2.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());
                }
                g2.setComposite(old);
            }

            // drag line + force %
            if (dragStart != null && dragNow != null) {
                double dx = dragStart.x - dragNow.x;
                double dy = dragStart.y - dragNow.y;
                double raw = Math.hypot(dx, dy);
                double pct = Math.min(100.0, raw / (MAX_FORCE * 8.0) * 100.0);
                g2.setColor(Color.WHITE);
                g2.drawLine(dragStart.x, dragStart.y, dragNow.x, dragNow.y);
                g2.drawString("Force: " + (int) pct + "%", dragNow.x + 12, dragNow.y - 6);
            }

            // display score & throwables top-left
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            int y = 26;
            for (Player p : game.players) {
                g2.drawString(p.name + "  Score: " + p.collected + "  Throwables: " + p.throwables, 10, y);
                y += 26;
            }

            // whose turn (top-right)
            Player cp = game.getCurrentPlayer();
            g2.drawString("Turn: " + cp.name, getWidth() - 160, 26);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (game == null) return;
            Player cp = game.getCurrentPlayer();
            if (cp instanceof BotPlayer) return;
            for (Marble m : game.marbles) {
                if (m.owner == cp && !m.moving()) {
                    double d = Math.hypot(e.getX() - m.x, e.getY() - m.y);
                    if (d <= m.R) {
                        selected = m;
                        dragStart = new Point((int) m.x, (int) m.y);
                        dragNow = e.getPoint();
                        break;
                    }
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selected != null) dragNow = e.getPoint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (selected != null && dragStart != null && dragNow != null) {
                double dx = dragStart.x - dragNow.x;
                double dy = dragStart.y - dragNow.y;
                double speed = Math.min(MAX_FORCE, Math.hypot(dx, dy) / 8.0);
                double angle = Math.atan2(dy, dx);
                selected.vx = speed * Math.cos(angle);
                selected.vy = speed * Math.sin(angle);
                selected.lastTouchedBy = game.getCurrentPlayer();
                // decrement throwable immediately when shot
                if (selected.owner != null) selected.owner.useThrowable();

                // clear selection and mark turn end
                selected = null; dragStart = null; dragNow = null;
                turnShot = true;
            }
        }

        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseMoved(MouseEvent e) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HolenGame::new);
    }
}