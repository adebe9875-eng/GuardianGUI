// Guardian GUI - a window with buttons that checks the programs running on your computer.
// It does NOT run any new program. It only reads information about running programs.
// It flags:
//   - programs running with admin/root rights
//   - programs running from temp or downloads folders
//   - programs whose SHA-256 hash is in known_hashes.txt
// Note: this is an educational tool. It does NOT detect every virus.

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GuardianGUI extends JFrame {

    static final String HASH_FILE = "known_hashes.txt";
    static final String[] RISKY_FOLDERS = {"\\temp\\", "\\downloads\\", "/tmp/", "/var/tmp/", "/downloads/"};

    static final Color LOW_COLOR = new Color(0xD5F5E3);
    static final Color MEDIUM_COLOR = new Color(0xFCF3CF);
    static final Color HIGH_COLOR = new Color(0xFADBD8);
    static final Color PRIMARY = new Color(0x14396B);
    static final Color ACCENT = new Color(0x2E86DE);

    private final DefaultTableModel model;
    private final JLabel totalLabel = new JLabel("-");
    private final JLabel highLabel = new JLabel("-");
    private final JLabel mediumLabel = new JLabel("-");
    private final JLabel timeLabel = new JLabel("-");
    private final JLabel statusLabel = new JLabel("Press \"Start Scan\" to check the running programs.");
    private final JButton scanButton = new JButton("Start Scan");
    private final JButton clearButton = new JButton("Clear");

    public GuardianGUI() {
        super("Guardian");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel title = new JLabel("Guardian");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Checks the programs running on this computer");
        subtitle.setForeground(new Color(0xBFE3FF));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        // Stats cards
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        stats.add(statCard("Programs checked", totalLabel, PRIMARY));
        stats.add(statCard("High risk", highLabel, new Color(0xC0392B)));
        stats.add(statCard("Medium risk", mediumLabel, new Color(0xB9770E)));
        stats.add(statCard("Last scan", timeLabel, PRIMARY));

        // Results table
        String[] columns = {"Program", "PID", "Admin", "Risk", "Reasons", "Location"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(3).setCellRenderer(new RiskRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(250);
        table.getColumnModel().getColumn(5).setPreferredWidth(320);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));

        // Buttons and status
        scanButton.setBackground(ACCENT);
        scanButton.setForeground(Color.WHITE);
        scanButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        scanButton.addActionListener(e -> startScan());

        clearButton.addActionListener(e -> clearResults());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttons.add(scanButton);
        buttons.add(clearButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(buttons, BorderLayout.NORTH);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 10, 16));
        bottom.add(statusLabel, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(header, BorderLayout.NORTH);
        top.add(stats, BorderLayout.SOUTH);
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JPanel statCard(String label, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD6E4F0)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(color);
        JLabel name = new JLabel(label);
        name.setForeground(new Color(0x5B6675));
        card.add(valueLabel);
        card.add(name);
        return card;
    }

    private void clearResults() {
        model.setRowCount(0);
        totalLabel.setText("-");
        highLabel.setText("-");
        mediumLabel.setText("-");
        timeLabel.setText("-");
        statusLabel.setText("Results cleared.");
    }

    private void startScan() {
        scanButton.setEnabled(false);
        statusLabel.setText("Scanning... please wait.");
        model.setRowCount(0);

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                return scanPrograms();
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    int high = 0, medium = 0;
                    for (Object[] row : rows) {
                        model.addRow(row);
                        String risk = (String) row[3];
                        if (risk.equals("High")) high++;
                        if (risk.equals("Medium")) medium++;
                    }
                    totalLabel.setText(String.valueOf(rows.size()));
                    highLabel.setText(String.valueOf(high));
                    mediumLabel.setText(String.valueOf(medium));
                    timeLabel.setText(java.time.LocalTime.now().withNano(0).toString());
                    statusLabel.setText("Scan finished. " + rows.size() + " programs checked.");
                } catch (Exception ex) {
                    statusLabel.setText("Scan failed: " + ex.getMessage());
                } finally {
                    scanButton.setEnabled(true);
                }
            }
        }.execute();
    }

    static List<Object[]> scanPrograms() {
        Set<String> known = loadKnownHashes();
        Set<String> checked = new HashSet<>();
        List<Object[]> rows = new ArrayList<>();

        for (ProcessHandle process : ProcessHandle.allProcesses().collect(Collectors.toList())) {
            Optional<String> command = process.info().command();
            if (command.isEmpty()) {
                continue;
            }
            String path = command.get();
            if (!checked.add(path)) {
                continue;
            }

            boolean admin = isAdmin(process.info().user());
            boolean risky = isRiskyFolder(path);
            boolean knownBad = false;
            try {
                knownBad = known.contains(sha256(path));
            } catch (Exception ignored) {
                // some programs cannot be read, skip the hash check for them
            }

            List<String> reasons = new ArrayList<>();
            if (knownBad) reasons.add("hash is in known_hashes.txt");
            if (risky) reasons.add("runs from a temp or downloads folder");
            if (admin && risky) reasons.add("admin/root rights from a risky folder");

            String risk;
            if (knownBad || (admin && risky)) {
                risk = "High";
            } else if (risky) {
                risk = "Medium";
            } else {
                risk = "Low";
            }

            Path p = Paths.get(path);
            String name = p.getFileName() == null ? path : p.getFileName().toString();
            rows.add(new Object[]{
                    name,
                    process.pid(),
                    admin ? "Yes" : "No",
                    risk,
                    reasons.isEmpty() ? "-" : String.join("; ", reasons),
                    path
            });
        }

        // Show the most dangerous programs first
        rows.sort((a, b) -> riskOrder((String) a[3]) - riskOrder((String) b[3]));
        return rows;
    }

    static int riskOrder(String risk) {
        switch (risk) {
            case "High": return 0;
            case "Medium": return 1;
            default: return 2;
        }
    }

    static boolean isAdmin(Optional<String> user) {
        if (user.isEmpty()) return false;
        String u = user.get();
        return u.equalsIgnoreCase("root") || u.toUpperCase().contains("SYSTEM");
    }

    static boolean isRiskyFolder(String path) {
        String lower = path.toLowerCase();
        for (String folder : RISKY_FOLDERS) {
            if (lower.contains(folder)) return true;
        }
        return false;
    }

    static Set<String> loadKnownHashes() {
        Set<String> hashes = new HashSet<>();
        try {
            if (!Files.exists(Paths.get(HASH_FILE))) return hashes;
            for (String line : Files.readAllLines(Paths.get(HASH_FILE))) {
                String clean = line.trim().toLowerCase();
                if (!clean.isEmpty() && !clean.startsWith("#")) hashes.add(clean);
            }
        } catch (Exception ignored) {
            // no hash file, that is fine
        }
        return hashes;
    }

    static String sha256(String path) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(path));
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    // Colors each row's risk cell: green for Low, yellow for Medium, red for High
    static class RiskRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected) {
                String risk = String.valueOf(value);
                if (risk.equals("High")) c.setBackground(HIGH_COLOR);
                else if (risk.equals("Medium")) c.setBackground(MEDIUM_COLOR);
                else c.setBackground(LOW_COLOR);
            }
            setHorizontalAlignment(CENTER);
            return c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GuardianGUI().setVisible(true));
    }
}
