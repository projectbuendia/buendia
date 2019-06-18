# Buendia Access and Credentials

This file contains instructions to access credentialed resources for the Buendia Medical Records project.

The companion file, credentials.txt.gpg, contains usernames and passwords to access these resources. To decrypt and access the data in this file follow the steps listed below. 

Ensure you have a fresh pull of the buendia repo containing the "recovery" folder. 
Recovery will contain any project pertinent encrypted files.

Navigate to the recovery folder from your terminal. In order to decrypt any one file you will need the file name. 

Decrypt file using: $gpg --decrypt filename.txt.gpg

When prompted enter passphrase:
    (You should know the passphrase, if not ask one of the following people: Ping, Ivan, Jo, or Schuyler)

The recovery folder should now contian a new file with the same ’filename.txt’ without the .gpg tag. 

If so, it is now safe to erase original encrypted file: 
    $rm filename.txt.gpg
Modify plain text file
Save modifications to the text file.
Encrypt the current filename.txt file using: 
    $gpg -C filename.txt

The recovery folder should now contian a new file with the same ’filename.txt’ with the .gpg tag at the end.  Ie. filename.txt.gpg:
If so, delete the plain text file: 
    $rm filename.txt 

check the git status: 
    $git status 

*Git should list only one file as new if you followed the above steps.* 

Push the newly encrypted file to github: 
    $git commit -a -m “update to credentials recovery”


# Cloud Accounts
## Digital Ocean
We have a Team account at Digital Ocean. It is paid by Ivan's credit card. To log into it as admin, use the username and password provided in the credentials file.

# Google Drive
## 

