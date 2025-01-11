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
    imports ? [],
  }: ''
    cat ${writeText "${name}.java" (lib.concatStringsSep "\n" [
      ''
        package com.craftinginterpreters.lox;
        ${lib.concatMapStringsSep "\n" (i: "import ${i};") imports}

        public interface ${name} { // extends Grammar
          abstract <R> R accept(Visitor<R> visitor);

          interface Visitor<R> { // extends Grammar.Visitor<R>''
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
        # @Override
        # public <R> R accept(Grammar.Visitor<R> visitor) {
        #   if (visitor instanceof Visitor<R> v) {
        #     return v.visit${record}${name}(this);
        #   }
        #   throw new IllegalArgumentException("Unsupported visitor type: " + visitor.getClass());
        # }
        records)
      "}"
    ])}'';
in
  writeScriptBin "generate-grammar-classes" ("#!${lib.getExe bash}\n"
    # + ''
    #   echo "src/Grammar.java"
    #   cat ${writeText "Grammar.java" ''
    #     package com.craftinginterpreters.lox;
    #
    #     public interface Grammar {
    #       abstract <R> R accept(Visitor<R> visitor);
    #
    #       interface Visitor<R> {}
    #     }
    #   ''} > src/Grammar.java
    # ''
    + (lib.concatMapStringsSep "\n" (c: ''
        echo "src/${c.name}.java"
        ${gen-class c} > src/${c.name}.java
      '')
      classes)
    + # src/Grammar.java \
    ''
      ${lib.getExe google-java-format} -i \
        ${lib.concatMapStringsSep " " (c: "src/${c.name}.java") classes}
    '')
