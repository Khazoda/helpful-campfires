@echo off
echo Processing mod resources...
echo.

:: Process PNG files in resources folder and all subfolders
for /r ".\common\src\main\resources" %%f in (*.png) do (
    echo Processing: "%%f"
    "Z:\Repository Supplements\OxiPNG\oxipng" -o max --alpha --strip all -Z "%%f"
)

PAUSE