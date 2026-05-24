#!/usr/bin/env bash
# Ristretto · one-shot installer for the trio (jdp + xpresso + macc) + ristretto itself.
# usage:
#   curl -fsSL https://raw.githubusercontent.com/Nandobez/Ristretto/main/install.sh | bash
set -euo pipefail

PREFIX="${RISTRETTO_PREFIX:-$HOME/.local}"
REF="${RISTRETTO_REF:-main}"
CACHE="${RISTRETTO_CACHE:-$HOME/.cache/ristretto}"
WITH_TRIO=1

for arg in "$@"; do
  case "$arg" in
    --prefix=*)  PREFIX="${arg#--prefix=}" ;;
    --pin=*)     REF="${arg#--pin=}" ;;
    --cache=*)   CACHE="${arg#--cache=}" ;;
    --solo)      WITH_TRIO=0 ;;
    -h|--help)
      sed -n '2,9p' "$0"; exit 0 ;;
  esac
done

bold()  { printf '\033[1m%s\033[0m\n' "$*"; }
dim()   { printf '\033[2m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
red()   { printf '\033[31m%s\033[0m\n' "$*" >&2; }

need() { command -v "$1" >/dev/null || { red "✗ missing '$1' in PATH"; return 1; }; }

bold "ristretto installer"
dim  "  prefix: $PREFIX"
dim  "  trio:   $([ $WITH_TRIO -eq 1 ] && echo 'yes' || echo 'no (--solo)')"
echo

MISSING=0
need git  || MISSING=1
need mvn  || MISSING=1
need java || MISSING=1
[ "$MISSING" -eq 1 ] && { red "install git + mvn + jdk17+ first."; exit 1; }

JV=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
[ "${JV:-0}" -lt 17 ] && { red "✗ JDK 17+ required (found: $JV)"; exit 1; }
dim "✓ jdk $JV"

BINDIR="$PREFIX/bin"
mkdir -p "$BINDIR"

install_one() {
  local name="$1" url="$2"
  bold "[$name] installing"
  curl -fsSL "$url" | bash -s -- --prefix="$PREFIX"
  echo
}

if [ "$WITH_TRIO" -eq 1 ]; then
  install_one "jdp"     "https://raw.githubusercontent.com/Nandobez/jdp/main/install.sh"
  install_one "xpresso" "https://raw.githubusercontent.com/Nandobez/Xpresso/main/install.sh"
  install_one "macc"    "https://raw.githubusercontent.com/Nandobez/Macchiato/main/install.sh"
fi

# ----- ristretto itself -----
mkdir -p "$CACHE"
SRC="$CACHE/src"
REPO="${RISTRETTO_REPO:-https://github.com/Nandobez/Ristretto.git}"
if [ -d "$SRC/.git" ]; then
  dim "↻ updating $SRC"
  git -C "$SRC" fetch --quiet origin "$REF"
  git -C "$SRC" reset --quiet --hard "origin/$REF" 2>/dev/null || git -C "$SRC" checkout --quiet "$REF"
else
  dim "↓ cloning $REPO"
  rm -rf "$SRC"
  git clone --quiet --depth=1 --branch "$REF" "$REPO" "$SRC" 2>/dev/null \
    || git clone --quiet --depth=1 "$REPO" "$SRC"
fi

bold "building ristretto…"
(cd "$SRC" && mvn -q -DskipTests package)

JAR="$SRC/target/ristretto.jar"
[ -f "$JAR" ] || { red "✗ build produced no target/ristretto.jar"; exit 1; }

LIBDIR="$PREFIX/share/ristretto"
mkdir -p "$LIBDIR"
cp "$JAR" "$LIBDIR/ristretto.jar"

# Three wrappers: ristretto, rist, r
for name in ristretto rist r; do
  cat > "$BINDIR/$name" <<EOF
#!/usr/bin/env bash
exec java -jar "$LIBDIR/ristretto.jar" "\$@"
EOF
  chmod +x "$BINDIR/$name"
done

echo
green "✓ ristretto installed."
dim   "  aliases: ristretto · rist · r"
echo
case ":$PATH:" in
  *:"$BINDIR":*) : ;;
  *) dim "  $BINDIR is not on PATH — add to your shell rc:"; echo "    export PATH=\"$BINDIR:\$PATH\"" ;;
esac
dim "  try:  r --help"
