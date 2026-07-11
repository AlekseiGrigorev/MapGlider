Add-Type -AssemblyName System.Drawing
$path = "C:\Users\KATANA\AndroidStudioProjects\MapGlider\google_play_icon_512.jpg"
$width = 512
$height = 512
$bmp = New-Object System.Drawing.Bitmap $width, $height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

$factor = 512.0 / 108.0

# Background #2196F3
$bgBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 33, 150, 243))
$g.FillRectangle($bgBrush, 0, 0, $width, $height)

# Waves #4FC3F7
$wavePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 79, 195, 247)), ($factor)

function DrawWave($y1, $y2) {
    $p1x = 20.0 * $factor
    $p1y = $y1 * $factor
    $p2x = 54.0 * $factor
    $p2y = ($y1 - 10.0) * $factor
    $p3x = 88.0 * $factor
    $p3y = $y1 * $factor

    # Quadratic Bezier to Cubic Bezier conversion
    $c1x = $p1x + (2.0/3.0) * ($p2x - $p1x)
    $c1y = $p1y + (2.0/3.0) * ($p2y - $p1y)
    $c2x = $p3x + (2.0/3.0) * ($p2x - $p3x)
    $c2y = $p3y + (2.0/3.0) * ($p2y - $p3y)

    $g.DrawBezier($wavePen, $p1x, $p1y, $c1x, $c1y, $c2x, $c2y, $p3x, $p3y)
}

DrawWave 40.0
DrawWave 60.0
DrawWave 80.0

# Glider (White)
$gliderBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$points = @(
    (New-Object System.Drawing.PointF (54.0 * $factor), (25.0 * $factor)),
    (New-Object System.Drawing.PointF (85.0 * $factor), (75.0 * $factor)),
    (New-Object System.Drawing.PointF (54.0 * $factor), (65.0 * $factor)),
    (New-Object System.Drawing.PointF (23.0 * $factor), (75.0 * $factor))
)
$g.FillPolygon($gliderBrush, $points)

$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Jpeg)
$g.Dispose()
$bmp.Dispose()
Write-Host "Icon saved to $path"
