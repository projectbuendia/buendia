status() { code=$?; if [ $code != '0' ]; then echo "($code) "; fi; }
PATH=/usr/local/bin:$PATH
PS1='$(status)\[\033[33m\]\h[\!]\$\[\033[0m\] '
export LS_OPTIONS='--color=auto'
alias ls='ls $LS_OPTIONS'
alias ll='ls -l $LS_OPTIONS'
