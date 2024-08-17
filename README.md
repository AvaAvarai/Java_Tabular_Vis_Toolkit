# JTabViz: Java Tabular Visualization Toolkit

**JTabViz** is a Java-based machine learning data analysis toolkit designed for visualization and analysis of tabular machine learning data. Supports data augmentation features, users can export augmented data to explore how changes in the dataset affect their resultantly trained models. Load, view, analyze, and classify CSV data with normalization, visualization (heatmaps, Parallel, Shifted Paired, Static Circular, and Star coordinates, covariance matrix heatmap), row manipulation, cell editing, inequality-based classification rules testing, pure single-attribute region automatic discovery, feature engineering, attribute sorting, and data export.

Software tested on Windows (Windows 10/11), Linux (Pop OS!), Mac OS (Sonoma).

## Analyzing Fisher Iris Benchmark Dataset

These screenshots demonstrate exploration of the Fisher Iris dataset which we utilize as a benchmark.

98.67% accurate classifier exploring causal relations of the two remaining misclassified cases, one in Virginica and one in Versicolor classes, shown highlight in Shifted Paired Coordinates with the classification function duplicated for the third plot axes pair.
![Demo screenshot 6](screenshots/demo/Iris_Demo_6.png)

Highlighting sepal width outliers for Virginica class by rearranging axes to accentuate monotonic patterns, sorting by the desired attribute, and ctrl-clicking outlier cases to then visualize as shown here in Parallel Coordinates.
![Demo screenshot 1](screenshots/demo/Iris_Demo_1.png)

Highlighting sepal length outliers for the Versicolor class which remain difficult to classify in Shifted Paired Coordinates.
![Demo screenshot 2](screenshots/demo/Iris_Demo_2.png)

Static Circular Coordinates Demo highlighting a petal length outlier in the Virginica class.
![Demo screenshot 5](screenshots/demo/Iris_Demo_5.png)

Combined View Demo.
![Demo screenshot 3](screenshots/demo/Iris_Demo_3.png)

Outlier in Setosa sepal width attribute highlighted.
![Demo screenshot 4](screenshots/demo/Iris_Demo_4.png)

Versicolor cases conflicting in petal length attibute selected and highlighted in Star Coordinates.
![Demo screenshot 6](screenshots/demo/Iris_Demo_7.png)

Sorting attributes by covariance against sepal width, highlighting four outstanding cases in PC and SPC.
![Demo screenshot 6](screenshots/demo/Iris_Demo_8.png)

Rules Tester Demo.
![Rules Tester Demo screenshot](screenshots/demo/Rules_Test_Demo_1.png)

Classifying Fisher Iris data with a single attribute for 75% of dataset.
![Rules Tester Demo screenshot](screenshots/demo/Rules_Test_Demo_2.png)

## Analyzing Higher Dimensionality

Visualizing the MNIST letters train data in Shifted Paired Coordinates.
![SPC Demo screenshot](screenshots/demo/Mnist_Train_Demo_1.png)

Visualizing the Wisconsin Breast Cancer 30 feature data in Parallel Coordinates.
![PC demo screenshot](screenshots/demo/WBC_30_Demo_1.png)

Visualizing the Wisconsin Breast Cancer 30 feature data in Star Coordinates.
![Star demo screenshot](screenshots/demo/WBC_30_Demo_2.png)

Visualizing the Musk molecule 166 feature data in Parallel Coordinates.
![PC demo screenshot](screenshots/demo/Musk_166_Demo_1.png)

Sorting by covariances of attribute v7 against all other features still in Parallel Coordinates.
![PC demo screenshot](screenshots/demo/Musk_166_Demo_2.png)

Hiding the classifiable cases with single attribute pure intervals which cover 5% threshold of class or dataset visualized in Parallel Coordinates.
![PC demo screenshot](screenshots/demo/Musk_166_Demo_3.png)

Visualizing the Musk molecule 166 feature data in Shifted Paired Coordinates.
![PC demo screenshot](screenshots/demo/musk_166_Demo_4.png)

Sorting by covariances of attribute v7 against all other features still in Shifted Paired Coordinates.
![PC demo screenshot](screenshots/demo/musk_166_Demo_5.png)

Hiding the classifiable cases with single attribute pure intervals which cover 5% threshold of class or dataset visualized in Shifted Paired Coordinates.
![PC demo screenshot](screenshots/demo/musk_166_Demo_6.png)

## Features

- Cross-platform support on headed platforms with Java.
- Load and display CSV data in tabular view
- Screenshot plots with space bar (currently works only on SPC, adding to other plots)
- Normalize numerical columns
- Highlight missing data
- Covariance matrix with heatmap overlay
- Covariance sort opens dialog, select an attribute and sort columns by the covariances of all other attributes against the selected attribute.
- Visualize data with heatmap overlay, Parallel Coordinates (PC), Shifted Paired Coordinates (SPC), Static Circular Coordinates (SCC), Star Coordinates
- Manipulate rows (insert, delete, clone, copy contents of selection, edit individual cell values)
- Customize font color and highlight class fields
- Export modified data to CSV
- Test classification rules displaying results in Confusion Matrix
- Serialize classification rules to reload
- Highlight selected rows in visualization views (PC, SPC, SCC, Star)
- Analytical single attribute pure region discovery algorithm, i.e. Attribute: petal.length, Pure Region: 0.00 <= petal.length < 0.15, Class: Setosa, Count: 50 (100.00% of class, 33.33% of dataset)
- Rules combinable to keep largest surrounding pure rule
- Cases classifiable with single pure attribute intervals hideable to simplify classifcation problem
- Rule threshold slider to specify required size of rule coverage over class or total dataset.
- Feature column insertion with direct trigonometric attribute values, i.e. arccos(attribute)
- Feature column insertion with forward x[n+1] – x[n] and backward x[n] – x[n-1] differences between attributes wrapped in trigonometric functions:
   This helps uncover various patterns and interactions in the data between attributes.
  - arcsin: Highlights small differences between attributes, making it easier to detect subtle variations.
  - arctan: Emphasizes the slope or rate of change between attributes, useful for understanding trends and gradients.
  - arccos: Focuses on rotational relationships between attributes, revealing how one attribute rotates relative to another.
- Overlay single attribute rule regions on Parallel Coordinates to visualize regions as shaded rectangular regions
- Inserted columns can be deleted by double-left clicking on their header.
- Feature column engineering with insertion of linear combination column, with custom coefficients per attribute, optionally wrapped in a trigonometric function.
- Gradient descent search for coefficients which optimize score function designed to maximize between-class variance and minimize within-class variance (fast and automatic quality coefficient discovery.)

## Automatic Rule Discovery

For classifying of data we automatically discover pure regions, intervals within a single attribute where all data points belong to the same class.

### Single Attribute Pure Intervals

Using a sliding window algorithm we automatically identify pure intervals within individual attributes based on a single attribute. After identifying all potential pure regions, JTabViz filters them to ensure only the most significant regions are used for classification, eliminating regions which contain another or if the threshold slider is not met for total percentage of class or dataset contained.

## Trigonometric Differences

To better analyze relationships between attributes, JTabViz applies trigonometric functions to the forward differences, backward differences, and direct attribute values. This helps uncover various patterns and interactions in the data between attributes.

- arcsin: Highlights small differences between attributes, making it easier to detect subtle variations.
- arctan: Emphasizes the slope or rate of change between attributes, useful for understanding trends and gradients.
- arccos: Focuses on rotational relationships between attributes, revealing how one attribute rotates relative to another.

## Feature Engineering with Linear Combinations for Data Classification

### Overview

In data classification, feature engineering transforms raw data into a more suitable form for building models. **JTabViz** includes a technique to create **Linear Combination Features** from existing attributes in your dataset. This method allows for the synthesis of new features that can reveal patterns and relationships not immediately visible in the original data.

### What Are Linear Combination Features?

A **Linear Combination Feature** is a new feature created by combining multiple existing features using a set of coefficients. It is mathematically represented as:

New Feature} = c_1(x_1) + c_2(x_2) + ... + c_n(x_n)

where:
c_1, c_2, ..., c_n are the coefficients.  
x_1, x_2, ..., x_n are the original features.

These coefficients can be manually specified or automatically optimized using gradient descent.

### Why Use Linear Combination Features?

Linear Combination Features are used to:

- **Enhance Class Separability**: Combining features can create new dimensions where different classes are more distinguishable.
- **Reduce Dimensionality**: A well-crafted linear combination can summarize the essential information of multiple features into a single feature.
- **Improve Model Representation**: Creating new features based on the relationships between existing features can make the data more suitable for classification.

This feature is inspired by the successful dynamic coordinate mappings of General Line Coordinats developed at Central Washington University.

### Automatic Coefficient Optimization with Gradient Descent

JTabViz includes a gradient descent algorithm that automatically finds the optimal coefficients for your linear combination feature. The optimization process aims to:

- **Maximize Between-Class Variance**: Increase the distance between the centers of different classes.
- **Minimize Within-Class Variance**: Reduce the spread of data points within the same class.

This approach ensures that the resulting linear combination feature is effective in separating different classes.

### Wrapping Linear Combinations with Trigonometric Functions

JTabViz allows you to apply trigonometric functions like `sin`, `cos`, or `tan`, as well as their inverses, `arcsin`, `arccos`, or `arctan`, to the linear combination feature. These transformations can reveal non-linear relationships and rotational patterns in the data, which may not be visible in the original features.

For example:

- **Sin**: Emphasizes periodic or cyclic relationships between features.
- **Cos**: Highlights rotational patterns and relationships.
- **Tan**: Focuses on the slope or rate of change between features.
- **Arcsin**: Shows the angle whose sine is the linear combination value, often highlighting small differences.
- **Arccos**: Represents the angle whose cosine is the linear combination value, useful for rotational relationships.
- **Arctan**: Provides the angle whose tangent is the linear combination value, emphasizing the slope or direction.

### Practical Example

Suppose you have a dataset with features `x1`, `x2`, and `x3`, and you want to create a new feature that helps distinguish between classes. Using JTabViz, you might create a linear combination feature like:

New Feature = 0.5(x1) + 0.3(x2) - 0.2(x3)

If this linear combination is wrapped with the `cos` function, the new feature becomes:

Transformed Feature = cos(0.5(x1) + 0.3(x2) - 0.2(x3))

This transformed feature could provide better insight into the relationships between the features and improve the classification of data.

### Summary

The **Linear Combination Feature Engineering** tool in JTabViz allows you to combine existing features and optimize their relationships to create new features. This approach can help in finding derivations of the data which present classifiable patterns.

## Getting Started

There are multiple ways to run JTabViz:

1. **Using the Pre-Compiled JAR File:**
   - Download the `JTabViz.jar` file and the `libs` folder.
   - Open a terminal and navigate to the directory containing the JAR file.
   - Run the following command:

     ```sh
     java -cp ".;libs/*;JTabViz.jar" src.Main
     ```

2. **Compiling and Running from Source:**
   - Clone the repository:

     ```sh
     git clone https://github.com/AvaAvarai/jtabviz.git
     ```

   - Navigate to the project directory.
   - Compile the project:

     ```sh
     javac -d out -cp "libs/*" src/*.java
     ```

   - Run the compiled project:

     ```sh
     java -cp ".;libs/*" src.Main
     ```

   **Note:** For macOS and Linux, replace the semicolon (`;`) in the classpath (`-cp`) with a colon (`:`).

3. **Using the Provided Scripts:**
   - Run the appropriate script for your operating system to compile and execute the application in one step:
     - **macOS/Linux:** `compile_and_run_jtabviz.sh`
     - **Windows:** `compile_and_run_jtabviz.bat`

## Data Format

JTabViz accepts data in CSV (Comma-Separated Values) format. Here are the key points about the expected data format:

1. File Extension: The data file should have a .csv extension.

2. Data Types:
   - Numerical data is preferred for optimal visualization and analysis.
   - Non-numerical data and blank fields are also accepted.

3. Class Column:
   - A column representing the class or category of each data point is expected.
   - This column can appear in any position.
   - Should be titled 'class' (case-insensitive, so 'Class' or 'CLASS' are also acceptable).
   - The class column is used for color-coding and shape assignment in various visualizations.

4. Header Row:
   - The first row of the CSV file should contain column names.

5. Delimiter:
   - Values should be separated by commas.

Example CSV structure:

| x1    | x2    | x3    | x4    | x5    | class |
|-------|-------|-------|-------|-------|-------|
| 3.14  | 2.71  | 1.41  | 0.58  | 1.73  | A     |
| 2.22  | 4.44  | 3.33  | 1.11  | 5.55  | B     |
| 0.87  | 1.23  | 3.45  | 5.67  | 7.89  | A     |
| 9.99  | 8.88  | 7.77  | 6.66  | 5.55  | C     |
| 1.23  | 4.56  | 7.89  | 2.34  | 5.67  | B     |

## Acknowledgements

The user interface icons are sourced from [Font Awesome](https://fontawesome.com/search) using [fa2png tool](https://fa2png.app/).

The Java charts library used is [JFreeChart](https://www.jfree.org/jfreechart/).

## License

The JTabViz software is completely available free of charge for use both personal and commercial under the MIT license, see the `LICENSE` for full terms and details.
