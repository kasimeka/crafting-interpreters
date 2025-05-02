{
  nixConfig.bash-prompt-prefix = ''\[\e[0;31m\](java) \e[0m'';
  description = "JDK 23 env";

  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [
            (_: _: {
              jre = graalvmDrv;
              jdk = graalvmDrv;
            })
          ];
        };
        graalvmDrv = pkgs.graalvmPackages.graalvm-ce-musl;

        generate-grammar-classes = (pkgs.callPackage
          ./hack/generate-grammar-classes.nix {}) [
          {
            name = "Expr";
            imports = ["java.util.List" "java.util.Optional"];
            records = {
              Logical = "Expr left, Token operator, Expr right";
              Binary = "Expr left, Token operator, Expr right";
              Unary = "Token operator, Expr right";
              Grouping = "Expr expression";
              Literal = "Object value";
              If = "Expr condition, Expr first, Expr second";
              Variable = "Token name";
              Assign = "Token name, Expr value";
              Call = "Expr callee, Token paren, List<Expr> arguments";
              Function = "List<Token> params, Stmt.Block body";
            };
          }
          {
            name = "Stmt";
            imports = ["java.util.List" "java.util.Optional"];
            records = {
              Block = "List<Stmt> statements, boolean enclosedInLoop";
              Expression = "Expr expression";
              Print = "Expr expression";
              Var = "Token name, Optional<Expr> initializer";
              If = " Expr condition, Stmt.Block thenBranch, Optional<Stmt.Block> elseBranch";
              While = "Expr condition, Stmt.Block body";
              Break = "";
              Return = "Token keyword, Optional<Expr> value";
              Function = "Token name, Expr.Function definition";
            };
          }
        ];

        pname = "jlox";
        version = "0.0.0-dev";
        mainClass = "com.craftinginterpreters.lox.Lox";
        drv = pkgs.buildGraalvmNativeImage {
          inherit pname version graalvmDrv;
          src = "${jar}/share/java/${pname}.jar";
          extraNativeImageBuildArgs = ["--static" "--libc=musl" "-march=native"];
        };
        jar = pkgs.stdenv.mkDerivation {
          inherit pname version;
          src = ./src;

          buildInputs = [pkgs.jre];
          nativeBuildInputs =
            [self.packages.${system}.generate-grammar-classes]
            ++ (with pkgs; [
              jdk
              stripJavaArchivesHook
            ]);

          buildPhase = ''
            install -Dm644 -t src $src/*
            ${pkgs.lib.getExe self.packages.${system}.generate-grammar-classes}
            find src -name '*.java' -type f -exec javac -d build/ {} +
            (cd build && jar cvfe $out/share/java/${pname}.jar ${mainClass} *)
          '';
          installPhase = ''
            mkdir -p $out/bin
            cat <<EOF > $out/bin/${pname}
            #!usr/bin/env sh
            JAVA_HOME=${pkgs.jre} exec ${pkgs.jre}/bin/java -jar $out/share/java/${pname}.jar "\$@"
            EOF
            chmod +x $out/bin/${pname}
          '';
          meta.mainProgram = pname;
        };
      in {
        packages = {
          inherit generate-grammar-classes;
          default = self.packages.${system}.native;
          native = drv;
          jvm = jar;
        };

        devShell = pkgs.mkShell {
          inputsFrom = [jar drv];
          packages = with pkgs; [
            google-java-format
            checkstyle
            (java-language-server.overrideMavenAttrs (_: {
              buildOffline = true;
              mvnHash = "sha256-kSoWd3r37bK/MYG8FKj6Kj3Z2wlHrSsDv3NdxbvhsaA=";
              src = fetchFromGitHub {
                owner = "nya3jp";
                repo = "java-language-server";
                rev = "0b256dfbe5e126112a90b70537b46b4813be6b93";
                hash = "sha256-6lIEavMxuIaxT6WjlYinP4crSyyVuMMtsUHXuVhvBRM=";
              };
            }))
          ];
          shellHook = ''echo "with love from wrd :)"'';
        };
      }
    );
}
