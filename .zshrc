export OPENAI_API_KEY="your-api-key-here"
export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"

# The next line updates PATH for the Google Cloud SDK.
if [ -f '/Users/aaryannaidu/Downloads/google-cloud-sdk/path.zsh.inc' ]; then . '/Users/aaryannaidu/Downloads/google-cloud-sdk/path.zsh.inc'; fi

# The next line enables shell command completion for gcloud.
if [ -f '/Users/aaryannaidu/Downloads/google-cloud-sdk/completion.zsh.inc' ]; then . '/Users/aaryannaidu/Downloads/google-cloud-sdk/completion.zsh.inc'; fi

export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  # This loads nvm
[ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"  # This loads nvm bash_completion
export CFLAGS="-I/opt/homebrew/include"
export LDFLAGS="-L/opt/homebrew/lib"
export PATH="/opt/homebrew/opt/libpq/bin:$PATH"
# Local bin (only once!)
export PATH=/Users/aaryannaidu/.local/bin:$PATH

# Android Platform Tools
export PATH=$PATH:/Users/aaryannaidu/platform-tools

# Java (JDK 17) - use dynamic lookup
export JAVA_HOME=$(/usr/libexec/java_home -v17)
export PATH="$JAVA_HOME/bin:$PATH"
