#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="${1:-/tmp/mizu_test_media}"
mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR"

echo "=== Generating test subtitles ==="
cat << 'EOF' > sub_eng.srt
1
00:00:01,000 --> 00:00:05,000
English Subtitle Cue 1: Welcome to Mizu Player!

2
00:00:06,000 --> 00:00:15,000
English Subtitle Cue 2: Testing dual audio and multi-track subtitles.
EOF

cat << 'EOF' > sub_hin.srt
1
00:00:01,000 --> 00:00:05,000
हिंदी उपशीर्षक १: मिज़ू प्लेयर में आपका स्वागत है!

2
00:00:06,000 --> 00:00:15,000
हिंदी उपशीर्षक २: ऑडियो और सबटाइटल परीक्षण।
EOF

cat << 'EOF' > sub_ass.ass
[Script Info]
Title: Mizu Test ASS
ScriptType: v4.00+
Collisions: Normal
PlayDepth: 0

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,24,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,1,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,0:00:01.00,0:00:08.00,Default,,0,0,0,,{\c&H00FFFF&}Mizu Styled ASS Subtitle Cue 1
Dialogue: 0,0:00:09.00,0:00:18.00,Default,,0,0,0,,{\c&H00FF00&}Mizu Styled ASS Subtitle Cue 2
EOF

cp sub_eng.srt t_mp4.srt

echo "=== Generating t_mp4.mp4 (H.264 + 2 AAC audio tracks + 2 mov_text subtitles) ==="
ffmpeg -y \
  -f lavfi -i "testsrc=duration=20:size=640x360:rate=30" \
  -f lavfi -i "sine=frequency=440:duration=20" \
  -f lavfi -i "sine=frequency=880:duration=20" \
  -i sub_eng.srt \
  -i sub_hin.srt \
  -c:v libx264 -pix_fmt yuv420p \
  -c:a aac -b:a 128k \
  -c:s mov_text \
  -map 0:v:0 -map 1:a:0 -map 2:a:0 -map 3:s:0 -map 4:s:0 \
  -metadata:s:a:0 language=eng -metadata:s:a:0 title="English Tone" \
  -metadata:s:a:1 language=hin -metadata:s:a:1 title="Hindi Tone" \
  -metadata:s:s:0 language=eng -metadata:s:s:0 title="English Subs" \
  -metadata:s:s:1 language=hin -metadata:s:s:1 title="Hindi Subs" \
  t_mp4.mp4

echo "=== Generating t_mkv.mkv (H.264 + 2 audio tracks + embedded ASS + SRT) ==="
ffmpeg -y \
  -f lavfi -i "testsrc=duration=20:size=640x360:rate=30" \
  -f lavfi -i "sine=frequency=440:duration=20" \
  -f lavfi -i "sine=frequency=880:duration=20" \
  -i sub_ass.ass \
  -i sub_eng.srt \
  -c:v libx264 -pix_fmt yuv420p \
  -c:a aac \
  -c:s copy \
  -map 0:v:0 -map 1:a:0 -map 2:a:0 -map 3:s:0 -map 4:s:0 \
  -metadata:s:a:0 language=eng -metadata:s:a:0 title="English Tone" \
  -metadata:s:a:1 language=hin -metadata:s:a:1 title="Hindi Tone" \
  -metadata:s:s:0 language=eng -metadata:s:s:0 title="Styled ASS" \
  -metadata:s:s:1 language=eng -metadata:s:s:1 title="SRT Subtitles" \
  t_mkv.mkv

echo "=== Generating t_avi.avi (MPEG-4 part 2 + MP3 audio) ==="
ffmpeg -y \
  -f lavfi -i "testsrc=duration=15:size=640x360:rate=25" \
  -f lavfi -i "sine=frequency=500:duration=15" \
  -c:v mpeg4 -qscale:v 3 \
  -c:a mp3 -b:a 128k \
  t_avi.avi

echo "=== Generating t_audio.mp3 (with embedded cover art) ==="
ffmpeg -y -f lavfi -i "color=c=0x5CE1E6:s=300x300:d=1" -vframes 1 cover.jpg
ffmpeg -y \
  -f lavfi -i "sine=frequency=440:duration=15" \
  -i cover.jpg \
  -map 0:a -map 1:v \
  -c:a mp3 -b:a 192k \
  -c:v mjpeg -id3v2_version 3 \
  -metadata:s:v title="Album cover" -metadata:s:v comment="Cover (front)" \
  -metadata title="Mizu Test Tone" -metadata artist="Mizu Artist" -metadata album="Mizu Album" \
  t_audio.mp3

rm -f cover.jpg sub_eng.srt sub_hin.srt sub_ass.ass
echo "=== Test media generated successfully in $OUTPUT_DIR ==="
ls -lh "$OUTPUT_DIR"
