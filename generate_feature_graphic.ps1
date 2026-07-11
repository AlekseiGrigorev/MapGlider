Add-Type -AssemblyName System.Drawing
$path = "C:\Users\KATANA\AndroidStudioProjects\MapGlider\google_play_feature_graphic.jpg"
$width = 1024
$height = 500
$bmp = New-Object System.Drawing.Bitmap $width, $height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Colors
$bgColor = [System.Drawing.Color]::FromArgb(255, 33, 150, 243)
$waveColor = [System.Drawing.Color]::FromArgb(255, 79, 195, 247)
$gliderColor = [System.Drawing.Color]::White

# Background
$bgBrush = New-Object System.Drawing.SolidBrush $bgColor
$g.FillRectangle($bgBrush, 0, 0, $width, $height)

# Draw waves across the whole width
$wavePen = New-Object System.Drawing.Pen $waveColor, 10
for ($i = 0; $i -lt 5; $i++) {
    $yBase = 100 + $i * 80
    $p1x = 0
    $p1y = $yBase
    $p2x = $width / 2
    $p2y = $yBase - 30
    $p3x = $width
    $p3y = $yBase

    # Quadratic to Cubic conversion
    $c1x = $p1x + (2.0/3.0) * ($p2x - $p1x)
    $c1y = $p1y + (2.0/3.0) * ($p2y - $p1y)
    $c2x = $p3x + (2.0/3.0) * ($p2x - $p3x)
    $c2y = $p3y + (2.0/3.0) * ($p2y - $p3y)

    $g.DrawBezier($wavePen, $p1x, $p1y, $c1x, $c1y, $c2x, $c2y, $p3x, $p3y)
}

# Glider (Centered and larger)
# Original coords (108x108): 54,25; 85,75; 54,65; 23,75
# Let's scale it. Original height was 50 (75-25).
# For 500 height, let's make the glider about 300px high.
$scale = 6.0
$centerX = $width / 2
$centerY = $height / 2

# Relative offsets from center (54, 50 is approx center of original logo)
$points = @(
    (New-Object System.Drawing.PointF ($centerX + (54 - 54)*$scale), ($centerY + (25 - 50)*$scale)),
    (New-Object System.Drawing.PointF ($centerX + (85 - 54)*$scale), ($centerY + (75 - 50)*$scale)),
    (New-Object System.Drawing.PointF ($centerX + (54 - 54)*$scale), ($centerY + (65 - 50)*$scale)),
    (New-Object System.Drawing.PointF ($centerX + (23 - 54)*$scale), ($centerY + (75 - 50)*$scale))
)

$gliderBrush = New-Object System.Drawing.SolidBrush $gliderColor
$g.FillPolygon($gliderBrush, $points)

# Save
$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Jpeg)
$g.Dispose()
$bmp.Dispose()
Write-Host "Feature graphic saved to $path"
