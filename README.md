# DevLogin

A dead simple Microsoft Authentication implementation for Minecraft.
Intended for use in mod development environments.

Supports Java 8 and above. 1.12 and above are supported. (Older likely works)

### Usage and Installation

- Install // TODO Links to install guides.
- Start the game.
- You will see a prompt in the console with similar text to the following `To sign in, use a web browser to open the page https://www.microsoft.com/link and enter the code <code> to authenticate.`
- Proceed to https://www.microsoft.com/link enter the code and login to the Microsoft account. Your account must own a valid copy of Minecraft.
- Follow the prompts and link DevLogin to your account.
- Setup is complete. The game will now launch.

### Multiple accounts

Switching between multiple Minecraft accounts is supported via the `--launch_profile` command line argument.
With this argument omitted, the `default` profile will be used.

### Manual usage

- Add DevLogin jar to runtime classpath.
- Set 'Main Class' to `net.covers1624.devlogin.DevLogin`
- Add program argument `--launch_target <original main class>`

### Dependencies

Dev login has 2 dependencies it expects to find on the class path.
- Gson. Required for processing Json. This library is used by Minecraft and should already exist
- Apache `HttpClient` OR Java11+. If Java11 is found, `HttpClient` is not required.
  - If you have special requirements, you are free to implement your own `HttpEngine` and use the `devlogin.http_engine` system-property override.

### How it works

DevLogin is intended to wrap the main class used by your IDE/launch platform.
It will perform the login/validation flow and append the required launch arguments.

DevLogin stores certain tokens and private data in `~/.devlogin/` in plain text. This is to
facilitate using the same login accounts, and tokens across multiple dev environments, etc.

The specific data stored in this folder is:

- Microsoft Authentication access token. Used to access the XBoxLive APIs.
- Microsoft Authentication refresh token. Used to refresh the above access token.
- Minecraft access token. Used to access Minecraft apis. Provided to the game.

On each game startup these access tokens and their associated expiry timestamps are validated and refreshed
as required.

If you are not comfortable with this, or would like other options for storing the data (encryption, etc.)
I am open to PR's/discussions addressing this.

DevLogin appends the following program arguments before handing off to the launch target:
- `--accessToken <minecraft access token>`
- `--uuid <minecraft account UUID>`
- `--username <minecraft account username>`
- `--userType msa`

Builds can be found on maven [here](https://maven.covers1624.net/net/covers1624/DevLogin).
