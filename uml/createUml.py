import os
import re
import subprocess
from collections import defaultdict

# Base folder containing the Java files
folder_to_check = "../src"

# Recursively find all .java files
java_files = []
for dirpath, _, files in os.walk(folder_to_check):
    for file in files:
        if file.endswith(".java"):
            java_files.append(os.path.join(dirpath, file))

# Extract class and relationship information
info = []
relationships = defaultdict(list)

for file in java_files:
    with open(file, "r") as f:
        content = f.readlines()

    class_info = {}
    package_name = ""
    
    for line in content:
        # Detect package name
        if line.strip().startswith("package "):
            package_name = re.findall(r"package\s+([\w.]+)", line)[0]
            
        # Detect class definition
        if re.search(r"\bclass\b", line) and "{" in line:
            class_name = re.findall(r"class\s+(\w+)", line)
            if class_name:
                class_name = class_name[0]
                class_info = {
                    "class": class_name,
                    "file": file,
                    "package": package_name,
                    "functions": []
                }
                info.append(class_info)

            # Detect inheritance
            if "extends" in line:
                parent_class = re.findall(r"extends\s+(\w+)", line)
                if parent_class:
                    parent_class = parent_class[0]
                    relationships["inheritance"].append((parent_class, class_name))

            # Detect interface implementation
            if "implements" in line:
                interfaces = re.findall(r"implements\s+([\w, ]+)", line)
                if interfaces:
                    for interface in interfaces[0].split(","):
                        interface = interface.strip()
                        relationships["implementation"].append((interface, class_name))

        # Detect composition (class as a member variable)
        if re.search(r"\b(\w+)\s+\w+;", line):
            member_class = re.findall(r"\b(\w+)\s+\w+;", line)
            if member_class:
                for owned in member_class:
                    if owned not in ["int", "String", "double", "boolean", "char", "float", "long", "short"]:
                        relationships["composition"].append((class_info.get("class", ""), owned))

        # Detect aggregation (class as a collection or array)
        if re.search(r"\b(\w+)\s+\w+\[\];", line) or re.search(r"\b(\w+)\s+\w+;", line):
            aggregated_class = re.findall(r"\b(\w+)\s+\w+\[\];", line) or re.findall(r"\b(\w+)\s+\w+;", line)
            if aggregated_class:
                for aggregated in aggregated_class:
                    if aggregated not in ["int", "String", "double", "boolean", "char", "float", "long", "short"]:
                        relationships["aggregation"].append((class_info.get("class", ""), aggregated))

        # Detect public functions (excluding constructors and main method)
        if re.search(r"public\s+\w+\s+\w+\(.*\)", line) and "main" not in line:
            func_name = re.findall(r"public\s+\w+\s+(\w+)\(.*\)", line)
            if func_name and "functions" in class_info:
                class_info["functions"].append(func_name[0])

# Validate relationships and include referenced classes not in the project
valid_classes = {item["class"] for item in info}
all_classes = valid_classes.copy()
for key, pairs in relationships.items():
    for _, b in pairs:
        if b not in valid_classes:
            all_classes.add(b)

relationships = {
    key: [(a, b) for a, b in pairs if a in all_classes and b in all_classes]
    for key, pairs in relationships.items()
}

# Group classes by package
packages = defaultdict(list)
for item in info:
    packages[item.get("package", "default")].append(item)

# Generate PlantUML code
uml_code = "@startuml\n"
uml_code += "skinparam classAttributeIconSize 0\n"
uml_code += "skinparam class {\n"
uml_code += "\tBorderColor black\n"
uml_code += "}\n"

# Generate package and class definitions
for package_name, package_classes in packages.items():
    uml_code += f"package {package_name} {{\n"
    for item in package_classes:
        uml_code += f"\tclass {item['class']} {{\n"
        uml_code += f"\t\tFrom file: {os.path.basename(item['file'])}\n"
        for func in item.get("functions", []):
            uml_code += f"\t\t+ {func}()\n"
        uml_code += "\t}\n"
    uml_code += "}\n"

# Add relationships
for parent, child in relationships["inheritance"]:
    uml_code += f"{parent} <|-- {child}\n"
for interface, impl_class in relationships["implementation"]:
    uml_code += f"{interface} <|.. {impl_class}\n"
for owner, owned in relationships["composition"]:
    uml_code += f"{owner} *-- {owned}\n"  # Composition
for owner, aggregated in relationships["aggregation"]:
    uml_code += f"{owner} o-- {aggregated}\n"  # Aggregation

uml_code += "@enduml"

# Save UML to the current directory
uml_file = "generatedUML.txt"
with open(uml_file, "w") as f:
    f.write(uml_code)

# Generate SVG
try:
    subprocess.run(
        ["java", "-jar", "plantuml.jar", "-tsvg", uml_file],
        check=True,
    )
    print(f"UML diagram successfully generated: {uml_file}")
except subprocess.CalledProcessError as e:
    print(f"Error generating UML diagram: {e}")

# Save SVG image
svg_file = "generatedUML.svg"
with open(svg_file, "wb") as f:
    f.write(subprocess.check_output(["java", "-jar", "plantuml.jar", "-tsvg", uml_file]))

print(f"UML diagram images successfully generated: {svg_file}")
