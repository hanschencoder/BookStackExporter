# BookStackExporter

Support export from BookStack to Markdown, HTML, PDF, TXT and GitBook.

# Download

# Usage

```bash
usage: BookStackExporter [-f] [-h] -host <arg> [-o <arg>] [-t <arg>] -tokenId <arg> -tokenSecret <arg>
 -f,--force           Force overwrite
 -h,--help            Print help
 -host <arg>          Base url of BookStack
 -o,--output <arg>    Export dir
 -t,--type <arg>      File type, [gitbook|pdf|markdown|plaintext|html]
 -tokenId <arg>       This is a non-editable system generated identifier for this token which will need to be
                      provided in API requests.
 -tokenSecret <arg>   This is a system generated secret for this token which will need to be provided in API
                      requests. This will only be displayed this one time so copy this value to somewhere safe
                      and secure.
```

 - `-o`: Specify output dir
 - `-h`: Specify the address of your BookStack website
 - `-tokenId`: Can be obtained from the following ways: `Edit Profile` - `API Tokens` -  `Create API Token`
 - `-tokenSecret`: Can be obtained from the following ways: `Edit Profile` - `API Tokens` -  `Create API Token`
 - `-t`: Specify the file type, The options are the following: gitbook, pdf, markdown, plaintext, html
 - `-f`: Overwrite if specify output dir is exists

---

# License

Apache 2.0. See the [LICENSE](./LICENSE) file for details.
