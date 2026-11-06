img src="hares_logo_v2.svg" width="120" alt="hares_logo_v2.svg"/>

# Guardian

Guardian is an educational program-protection tool written in Java and Python. It inspects the programs running on your computer and warns you about risky ones.

> **Important:** Guardian does not detect every virus. No program can guarantee that. It is a learning project, so use a well-known security program to protect a real computer.

## Features

- **Graphical window (Java Swing):** buttons, a results table, and color-coded risk levels.
- **Admin rights check:** flags programs running as administrator (Windows) or root (Linux/macOS).
- **Risky location check:** flags programs running from temporary or downloads folders.
- **Known hash check:** flags programs whose SHA-256 fingerprint appears in `known_hashes.txt`.
- **Dashboard (HTML, CSS, JavaScript):** a web page that displays a report created by the Python monitor.
- **Download page:** `index.html` offers the Java file for download.

Guardian never runs any new program. It only reads information about the programs that are already running.

## Files

| File | Description |
| --- | --- |
| `GuardianGUI.java` | Graphical window with buttons and a results table |
| `Guardian.java` | Command-line version that checks running programs against known hashes |
| `guardian_monitor.py` | Python monitor that checks admin rights and risky folders, and writes `report.json` and `report.js` |
| `dashboard.html` | Web dashboard that displays the report from `report.js` |
| `index.html` | Download page for the project |
| `guardian_logo.svg` | Project logo |
| `known_hashes.txt` | Optional list of known SHA-256 fingerprints, one per line |

## Requirements

- **Java 9 or newer** for `GuardianGUI.java` and `Guardian.java`
- **Python 3** and the `psutil` library for `guardian_monitor.py`
- A computer with a graphical screen for the window version

Install `psutil` with:

```
pip install psutil
```

## How to run

### Graphical window

```
javac GuardianGUI.java
java GuardianGUI
```

Press **Start Scan** to check the running programs, and **Clear** to remove the results.

### Command-line version

```
javac Guardian.java
java Guardian
```

### Python monitor and dashboard

```
python guardian_monitor.py
```

Then open `dashboard.html` in your browser. It reads `report.js`, so it works by double-clicking the file.

## Known hashes

To add known threats, create a file named `known_hashes.txt` in the same folder. Put one SHA-256 fingerprint per line. Lines starting with `#` are comments and are ignored.

```
# Example
3a7bd3e2360a3d80c1a8e5d1e6f2b9c0d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9
```

Programs whose fingerprint matches this list are reported as high risk.

## Troubleshooting

- **`HeadlessException`:** the program has no graphical screen available. Run it from a desktop terminal, or on WSL, run `wsl --update` in PowerShell and restart.
- **`ModuleNotFoundError: No module named 'psutil'`:** run `pip install psutil`.
- **Dashboard is empty:** run `guardian_monitor.py` first, then refresh the page.

## Limitations

- Only programs listed in `known_hashes.txt` are detected by hash.
- Admin and risky-location checks are simple rules, so they can give false alarms or miss threats.
- Guardian is not a replacement for a professional antivirus program.

## License

Educational project. Feel free to learn from it, modify it, and share it.
