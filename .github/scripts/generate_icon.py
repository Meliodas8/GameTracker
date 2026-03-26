from PIL import Image, ImageDraw, ImageFont

img = Image.new("RGB", (256, 256), color=(74, 144, 217))
draw = ImageDraw.Draw(img)

try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 72)
except:
    font = ImageFont.load_default()

text = "GT"
bbox = draw.textbbox((0, 0), text, font=font)
x = (256 - (bbox[2] - bbox[0])) // 2 - bbox[0]
y = (256 - (bbox[3] - bbox[1])) // 2 - bbox[1]
draw.text((x, y), text, fill="white", font=font)
img.save("AppDir/usr/share/icons/hicolor/256x256/apps/gametracker.png")
