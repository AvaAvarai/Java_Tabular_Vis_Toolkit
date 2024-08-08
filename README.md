# JTabViz: Java Tabular Visualization Toolkit

**JTabViz** is a Java-based application for loading, viewing, augmenting, and classifying tabular machine learning data loaded from CSV files. The application provides features including normalization, heatmap visualization, row deleting, row insertion, field augmentation, Parallel Coordinates, rules testing, and data export.

We are exploring to see if we can add custom visualization techniques with the utilized libraries (Java Swing, JFreeChart).

Fisher Iris Data Exploration Demo.
![Demo screenshot](screenshots/Iris_Demo_1.png)

Rules Tester Demo.
![Rules Tester Demo screenshot](screenshots/Rules_Test_Demo_1.png)

## Features

- **Load CSV Data**: Load and display CSV data in a tabular format.
- **Normalize Data**: Toggle normalization of numerical columns in the dataset.
- **Highlight Blanks**: Toggle highlight of cell backgrounds with missing data.
- **Heatmap Visualization**: Toggle highlighting data rows backgrounds as a heatmap.
- **Parallel Coordinates Visualization**: Display a new window with data shown in a Parallel coordinates plot, normalization (or not) and augmentation carries over.
- **Row Manipulation**: Insert, delete, and augment (change value) rows, and visually reorder rows using drag and drop.
- **Font Color Customization**: Customize the font color of the table cells.
- **Tabular Class Field Highlight**: Toggle tabular class fields drawn with class color for background with class color selection.
- **Data Export**: Export the modified data back to a CSV file.
- **Rules Tester**: Tool window to test classification rules of the form: val1 R1 att R2 val2 i.e. 0.0 < petal.length < 2.0 rules can be chained in succession with conjunction (and) or disjunction (or) clauses, and effect one class.
- **Multi-row select and drag reorder**: Press shift and left click to select from the current row to the clicked row (total rows highlighted displayed in footer), can drag and reorder rows including entire shift-click selections.

## Getting Started

To run this project you need to compile to project along with the contained library in `libs\`. To do so there is three steps:

With our Pre-Compiled Jar:

1; Run from jar

```sh
java -cp ".;libs/*;JTabViz.jar" src.CsvViewer
```

Stand-alone:

1; Clone the project to your computer:

```sh
git clone git@github.com:AvaAvarai/jtabviz.git
```

2; Compile the project:

```sh
javac -cp ".;libs/*" src/CsvViewer.java
```

3; Execute the compiled project:

```sh
java -cp ".;libs/*" src/CsvViewer
```

With a Jar:

1; Compile a jar

```sh
jar cfm JTabViz.jar MANIFEST.MF -C out .
```

2; Run from jar

```sh
java -cp ".;libs/*;JTabViz.jar" src.CsvViewer
```

## Data Format

Data used should be stored in a .csv file, ideally numerical, however, this software should accept blank fields. The label column should be titled `class`, case-insensitive.

## Aknowledgements

The user interface icons used are source from Font Awesme.

## License

The software is freely available under the MIT license see `LICENSE` for full details.
