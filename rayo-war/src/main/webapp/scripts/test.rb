require 'java'

# Get the incoming call
current_call = $tropo.calls[0]

# Answer the call
current_call.answer()

# Say some stuff
$tropo.verb(current_call, com.tropo.core.verb.Say, java.util.HashMap.new({
  "promptItems" => "Thanks for installing Tropo"
})).start().get()
