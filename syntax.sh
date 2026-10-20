OUTFILE="syntax.txt"
INFILE="syntax"

find "$INFILE" -type f -exec sh -c '
	for file do
		printf "// ./%s\n// auto generated\n" "$file"
		cat "$file"
		printf "\n"
	done
' sh {} + > "$OUTFILE"