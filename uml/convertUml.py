#!/usr/bin/env python3
import os

def convert_svg_to_png():
    """Convert SVG to PNG using multiple possible methods."""
    # Get absolute paths
    current_dir = os.path.dirname(os.path.abspath(__file__))
    input_file = os.path.join(current_dir, "generatedUML.svg")
    output_file = os.path.join(current_dir, "generatedUML.png")

    if not os.path.exists(input_file):
        print(f"Input file not found: {input_file}")
        return False

    # Try different methods in order of preference
    methods = [
        try_cairosvg,
        try_imagemagick,
        try_inkscape,
        try_rsvg_convert
    ]

    for method in methods:
        try:
            if method(input_file, output_file):
                return True
        except Exception as e:
            print(f"Method {method.__name__} failed: {e}")
            continue
    
    return False

def try_cairosvg(input_file, output_file):
    """Try converting using cairosvg."""
    try:
        import cairosvg
        with open(input_file, 'rb') as svg_file:
            svg_content = svg_file.read()
            cairosvg.svg2png(bytestring=svg_content, write_to=output_file, background_color="white")
        print("Converted using cairosvg")
        return True
    except ImportError:
        print("cairosvg not available")
        return False

def try_imagemagick(input_file, output_file):
    """Try converting using ImageMagick."""
    import subprocess
    try:
        subprocess.run(['convert', input_file, output_file], check=True)
        print("Converted using ImageMagick")
        return True
    except (subprocess.SubprocessError, FileNotFoundError):
        print("ImageMagick not available")
        return False

def try_inkscape(input_file, output_file):
    """Try converting using Inkscape."""
    import subprocess
    try:
        subprocess.run(['inkscape', '-z', '-e', output_file, input_file], check=True)
        print("Converted using Inkscape")
        return True
    except (subprocess.SubprocessError, FileNotFoundError):
        print("Inkscape not available")
        return False

def try_rsvg_convert(input_file, output_file):
    """Try converting using rsvg-convert."""
    import subprocess
    try:
        subprocess.run(['rsvg-convert', '-f', 'png', '-o', output_file, input_file], check=True)
        print("Converted using rsvg-convert")
        return True
    except (subprocess.SubprocessError, FileNotFoundError):
        print("rsvg-convert not available")
        return False

if __name__ == "__main__":
    if convert_svg_to_png():
        print("Successfully converted SVG to PNG")
    else:
        print("Failed to convert SVG to PNG. Please install one of the following:")
        print("- cairosvg (pip install cairosvg)")
        print("- ImageMagick (brew install imagemagick)")
        print("- Inkscape (brew install inkscape)")
        print("- librsvg (brew install librsvg)")