import random,time,sys
quotes=["you can do it","keep moving forward","believe in yourself","every roll is a new chance"]
while True:
    minrolls,maxdice,maxrolls,yourmoney,option,quotes_unlocked,gun_unlocked=0,0,0,0,0,0,0
    devaccess=input("Your name? ")
    if devaccess=="ribitsundevre221":
        minrolls=int(input("minimum rolls mr dev? "))
        print(f"set the minimum rolls to {minrolls}")
        maxdice=int(input("maximum dice mr dev? "))
        print(f"set maximum dice to {maxdice}")
        maxrolls=int(input("maximum rollls mr dev? "))
        print(f"set maximum rolls to {maxrolls}")
        yourmoney=int(input("how much money mr dev? "))
        print(f"set money to {yourmoney}")
    else:print(f"welcome to ribi dice simulator {devaccess}")
    while option<1:
        rolldice=input("roll the dice? y/n: ").lower()
        if rolldice=="ribirunsetkork88":
            confirmed=True
            for i in range(3):
                if input(f"are you sure? confirmation {i+1}/3 (y/n): ").lower()!="y":confirmed=False;break
            if confirmed:print("game reset successful, returning to menu\n");break
            else:print("reset cancelled, returning to game\n")
        elif rolldice=="y":
            if quotes_unlocked:print(f"quote: {random.choice(quotes)}")
            print("rolling dice..")
            time.sleep(1.3)
            min_roll=1+minrolls
            max_roll=10+maxrolls
            if min_roll>max_roll:min_roll=max_roll
            dice_to_roll=1+maxdice
            rolls_list=[random.randint(min_roll,max_roll) for _ in range(dice_to_roll)]
            total_rolled=sum(rolls_list)
            rolls_display=" + ".join(str(r) for r in rolls_list)
            print(f"you rolled: {rolls_display} = {total_rolled}")
            yourmoney+=total_rolled
            print(f"current wallet: ${yourmoney}\n")
        elif rolldice=="n":
            print(f"\n--- welcome to the shop ---\nyou have ${yourmoney} to spend")
            while True:
                price_min=15+(minrolls*15)
                price_dice=30+(maxdice*40)
                price_max=45+(maxrolls*20)
                print(f"\navailable items:\n1. +min rolls (${price_min}) - owned: {minrolls}/10\n2. extra die (${price_dice}) - owned: {maxdice}/7\n3. +max rolls (${price_max}) - owned: {maxrolls}/50\n4. motivational quotes ($0) - owned: {quotes_unlocked}/1\n5. gun down the system ($2000) - owned: {gun_unlocked}/1\n6. exit game")
                choice=input("what would you like to buy? (1-6): ")
                if choice=="1":
                    if minrolls>=10:print("cant buy this, you hit the max buy limit")
                    elif yourmoney>=price_min:yourmoney-=price_min;minrolls+=1;print(f"you bought +min rolls, remaining wallet: ${yourmoney}")
                    else:print("not enough money")
                elif choice=="2":
                    if maxdice>=7:print("cant buy this, you hit the max buy limit")
                    elif yourmoney>=price_dice:yourmoney-=price_dice;maxdice+=1;print(f"you bought an extra die, remaining wallet: ${yourmoney}")
                    else:print("not enough money")
                elif choice=="3":
                    if maxrolls>=50:print("cant buy this, you hit the max buy limit")
                    elif yourmoney>=price_max:yourmoney-=price_max;maxrolls+=1;print(f"you bought +max rolls, remaining wallet: ${yourmoney}")
                    else:print("not enough money")
                elif choice=="4":
                    if quotes_unlocked>=1:print("cant buy this, you hit the max buy limit")
                    else:quotes_unlocked=1;print(f"you bought motivational quotes, remaining wallet: ${yourmoney}")
                elif choice=="5":
                    if gun_unlocked>=1:print("cant buy this, you hit the max buy limit")
                    elif yourmoney>=2000:
                        yourmoney-=2000;gun_unlocked=1
                        print("\nyou successfully bought gun down the system")
                        print("congratulations, you broke the simulation and beat the game")
                        print("you shot the system in the nuts and escaped the dice box!!!")
                        print("\n .-------. \n /   o   /| \n/_______/ | \n| o   o | / \n| o   o |/  \n'-------'   ")
                        sys.exit()
                    else:print("not enough money")
                elif choice=="6":print(f"you left the shop with ${yourmoney} dollars");break
                else:print("invalid choice, pick 1, 2, 3, 4, 5, or 6")
            print("\nreturning to the main game menu")
        else:print("option not recognised, maybe try y")
