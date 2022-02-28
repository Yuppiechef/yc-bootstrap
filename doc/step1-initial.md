# Step 1 - Initial Basic setup

Tag: https://github.com/yuppiechef/yc-bootstrap/tree/step1-initial

 - Calva getting started
 - Clojure getting started: https://clojure.org/guides/getting_started
    - Get 'clojure' CLI installed
 - Create `yc-bootstrap` folder
 - Open in vscode, create deps.edn, follow https://clojure.org/guides/deps_and_cli

You should now have a very basic project going - good time to get some git integration going -- Finish up with a setup where you have only a single project folder and single deps.edn, else things will get weird fast.

Enter the following in your .gitignore:

```gitignore
.clj-kondo/.cache
.cpcache
.lsp
.calva/output-window
.nrepl-port
```

Also, create a cljfmt.edn file with the contents:
```clojure
{:indents ^:replace
 {#".*" [[:inner 0]]}
 :remove-multiple-non-indenting-spaces? true
 :insert-missing-whitespace? true}
 ```

Then, go to VSCode settings and filter for `cljfmt` - set the `Calva > Fmt: Config Path` to `cljfmt.edn`. This will get the code formatted in a consistent way. I also found better mileage unchecking the `New Indent Engine` option.