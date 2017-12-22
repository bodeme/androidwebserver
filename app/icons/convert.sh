#!/bin/bash

icon="lws_ic"

for size in 24 36 48 72 96 144 192 512; do
    inkscape -z --export-background=#000000 --export-background-opacity=0 --export-png=${icon}_${size}.png --export-width=${size} --export-height=${size} $icon.svg
done

for res in ldpi mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    mkdir -p ./res/mipmap-$res
done

pngcrush -q ${icon}_36.png  ./res/mipmap-ldpi/${icon}.png; rm ${icon}_36.png
pngcrush -q ${icon}_48.png  ./res/mipmap-mdpi/${icon}.png; rm ${icon}_48.png
pngcrush -q ${icon}_72.png  ./res/mipmap-hdpi/${icon}.png; rm ${icon}_72.png
pngcrush -q ${icon}_96.png  ./res/mipmap-xhdpi/${icon}.png; rm ${icon}_96.png
pngcrush -q ${icon}_144.png ./res/mipmap-xxhdpi/${icon}.png; rm ${icon}_144.png
pngcrush -q ${icon}_192.png ./res/mipmap-xxxhdpi/${icon}.png; rm ${icon}_192.png
                            
pngcrush -q ${icon}_24.png  ./res/${icon}_24.png; rm ${icon}_24.png; mv ./res/${icon}_24.png ./
pngcrush -q ${icon}_512.png  ./res/${icon}_512.png; rm ${icon}_512.png; mv ./res/${icon}_512.png ./
