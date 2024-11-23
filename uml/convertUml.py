import cairosvg

# Define the paths for the input SVG and output PNG
input_svg_path = "C:/Users/Alice/Downloads/diagram-17211371090926262103.svg"  # Replace with your SVG file name
output_png_path = "diagram.png"  # Replace with your desired PNG file name

# Convert the SVG to PNG with a white background
try:
    cairosvg.svg2png(
        url=input_svg_path,
        write_to=output_png_path,
        background_color="white"  # Set the background to white
    )
    print(f"SVG successfully converted to PNG with a white background: {output_png_path}")
except Exception as e:
    print(f"An error occurred while converting SVG to PNG: {e}")
