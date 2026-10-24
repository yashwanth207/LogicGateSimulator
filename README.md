# LogicGrid - Digital Circuit Simulator

An interactive logic gate simulator written in pure Java SE and Swing. No external libraries.
Build circuits from switches, gates and lights, wire them together, and watch signals propagate live.

## Requirements

Java 11 or newer. A JDK is only needed to compile; the prebuilt jar runs on any JRE 11+.

## Run it

**VS Code**
1. Install the *Extension Pack for Java* (VS Code will offer it when you open the folder).
2. File > Open Folder... and choose this `LogicGateSimulator` folder.
3. Open `src/logicsim/LogicGateSimulator.java` and click **Run** above `main`,
   or press **F5** and pick *Run LogicGrid*.

**IntelliJ IDEA / Eclipse**
Create a new project from existing sources, mark `src` as the source root, and run
`logicsim.LogicGateSimulator`.

**Terminal**
```
./run.sh          # macOS / Linux  (builds, then launches)
run.bat           # Windows
java -jar dist/LogicGateSimulator.jar     # prebuilt jar, no JDK needed
```
`build.sh` / `build.bat` only compile and rebuild the jar.

## Controls

| Action | How |
| --- | --- |
| Add a component | Drag it from the library onto the canvas, or click it and then click the canvas |
| Move a component | Drag its body (snaps to a 10 px grid) |
| Connect | Drag from an output pin (right side) to an input pin (left side) |
| Toggle a switch | Click it |
| Remove a wire | Right-click the input pin it feeds |
| Delete a component | Right-click it, or select it and press Delete |
| Cancel placement or wiring | Esc |

Wires glow green and carry moving pulses when the signal is 1, and stay dark red at 0.
Unconnected inputs read as 0. Connections that would create a feedback loop are rejected,
because this simulator evaluates combinational logic only.

## Project layout

```
src/logicsim/
  LogicGateSimulator.java      entry point (main)
  engine/                      no Swing dependency
    LogicComponent.java        interface: boolean getOutput()
    LogicGate.java             abstract base, manages input nodes
    AndGate / OrGate / NotGate / XorGate
    InputSwitch / OutputLight
  ui/
    MainFrame, AppContent      window and layout, repaint loop
    CircuitCanvas              rendering, dragging, wiring, hit-testing
    PalettePanel               component library and actions
    GateRenderer, Theme        gate symbols, neon palette, drawing helpers
    HeaderPanel, StatusBar, NeonButton, ThinScrollBarUI, AppIcon, NodeType, CircuitNode
```

## Adding a new gate

1. Add a class in `engine/` that extends `LogicGate` (for example `NandGate`).
2. Add a constant to `ui/NodeType.java` and return the new engine object from `createLogic()`.
3. Add a symbol in `ui/GateRenderer.java` (or reuse an existing body shape).
4. Add it to the gate list in `ui/PalettePanel.java`.

## Troubleshooting

* **"java" is not recognized** - install a JDK (for example Temurin 17 or 21) and reopen the terminal.
* **VS Code shows no Run button** - wait for the Java extension to finish importing, then reopen the file.
* **Text looks different** - fonts fall back automatically (JetBrains Mono / Consolas / Menlo / DejaVu Sans Mono).
