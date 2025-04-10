#!/bin/bash

source_dir="docs"
target_dir="ai_export"

rm -rf $target_dir
mkdir -p $target_dir

function exportByExtension {
  find "$source_dir" -name "*.$1" -print0 | while IFS= read -r -d $'\0' file; do
    new_filename=$file
    new_filename=$(echo "$new_filename" | sed "s/$source_dir\///g") # Remove the leading `docs/`
    new_filename=$(echo "$new_filename" | sed "s/\//-/g")           # Replace `/` with `-`
    new_filename=$(echo "$new_filename" | sed "s/-index//g")        # Rename `example-index.md` -> `example.md`
    cp "$file" "$target_dir/$new_filename"
  done
}

exportByExtension "md"
exportByExtension "mdoc"
exportByExtension "mdx"