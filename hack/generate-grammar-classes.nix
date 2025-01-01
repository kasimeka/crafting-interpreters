{
  lib,
  writeText,
  writeScriptBin,
  bash,
  google-java-format,
}: classes: let
  gen-class = {
    name,
    records,
  }: ''
    cat ${writeText "${name}.java" (lib.concatStringsSep "\n" [
      ''
        package com.craftinginterpreters.lox;

        public interface ${name} {
          abstract <R> R accept(Visitor<R> visitor);

          interface Visitor<R> {''
      (lib.concatMapAttrsStringSep "\n" (record: _fields: ''
          R visit${record}${name}(${record} expr);
        '')
        records)
      "}"
      (lib.concatMapAttrsStringSep "\n" (record: fields: ''
          record ${record}(${fields}) implements ${name} {
            @Override
            public <R> R accept(Visitor<R> visitor) {
              return visitor.visit${record}${name}(this);
            }
          }
        '')
        records)
      "}"
    ])}'';
in
  writeScriptBin "generate-grammar-classes" ("#!${lib.getExe bash}\n"
    + (lib.concatMapStringsSep "\n" (c: ''
        echo "src/${c.name}.java"
        ${gen-class c} > src/${c.name}.java
      '')
      classes)
    + ''
      ${lib.getExe google-java-format} -i \
        ${lib.concatMapStringsSep " " (c: "src/${c.name}.java") classes}
    '')
