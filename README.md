# JTabViz: Java Tabular Visualization Toolkit

**JTabViz** is a Java-based application for loading, viewing, and augmenting tabular data from CSV files. The application provides various functionalities including normalization, heatmap visualization, row manipulation, and data export. We are exploring to see if we can add custom visualization techniques with the utilized libraries (Java Swing, JFreeChart).

## Features

- **Load CSV Data**: Load and display CSV data in a tabular format.
- **Normalize Data**: Toggle normalization of numerical columns in the dataset.
- **Highlight Blanks**: Toggle highlight of cell backgrounds with missing data.
- **Heatmap Visualization**: Toggle highlighting data rows backgrounds as a heatmap.
- **Parallel Coordinates Visualization**: Display a new window with data shown in a Parallel coordinates plot, normalization (or not) and augmentation carries over.
- **Row Manipulation**: Insert, delete, and augment (change value) rows, and visually reorder rows using drag and drop.
- **Font Color Customization**: Customize the font color of the table cells.
- **Data Export**: Export the modified data back to a CSV file.

## Getting Started

Compile: `javac -cp ".;libs/*" CsvViewer.java`

Execute: `java -cp ".;libs/*" CsvViewer`

## License

The software is freely available under the MIT license see `LICENSE` for full details.
